/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.data;

//TO DO 00.0076
//TO DO 00.0126

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import ch.elexis.arzttarife_schweiz.Messages;
import ch.elexis.core.constants.Preferences;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.interfaces.IOptifier;
import ch.elexis.core.data.interfaces.IVerrechenbar;
import ch.elexis.core.data.interfaces.IVerrechenbar.DefaultOptifier;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.data.TarmedKumulation.TarmedKumulationType;
import ch.elexis.data.TarmedLimitation.LimitationUnit;
import ch.elexis.data.importer.TarmedLeistungAge;
import ch.elexis.data.importer.TarmedReferenceDataImporter;
import ch.elexis.tarmedprefs.RechnungsPrefs;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Dies ist eine Beispielimplementation des IOptifier Interfaces, welches einige einfache Checks von
 * Tarmed-Verrechnungen durchführt
 * 
 * @author gerry
 * 
 */
public class TarmedOptifier implements IOptifier {
	// +++++ START minutes
	public static boolean doMinuteOptify = true;
	public static boolean doStripMinuteItemsFromTree = true;
	
	public static boolean optifierDisabled = false;
	// +++++ END minutes
	
	private static final String TL = "TL"; //$NON-NLS-1$
	private static final String AL = "AL"; //$NON-NLS-1$
	public static final int OK = 0;
	public static final int PREISAENDERUNG = 1;
	public static final int KUMULATION = 2;
	public static final int KOMBINATION = 3;
	public static final int EXKLUSION = 4;
	public static final int INKLUSION = 5;
	public static final int LEISTUNGSTYP = 6;
	public static final int NOTYETVALID = 7;
	public static final int NOMOREVALID = 8;
	public static final int PATIENTAGE = 9;
	public static final int EXKLUSIVE = 10;
	
	private static final String CHAPTER_XRAY = "39.02";
	private static final String DEFAULT_TAX_XRAY_ROOM = "39.2000";
	
	boolean bOptify = true;
	private Verrechnet newVerrechnet;
	private String newVerrechnetSide;
	
	// +++++ START minutes
	// *** the first  entry is first  5-minute-chunks for all patients - this will always be sent to this method
	// *** the second entry is middle 5-minute-chunks for 6-75 years
	// *** the third  entry is middle 5-minute-chunks for children/olders (max 4*)
	// *** the fourth entry is middle 5-minute-chunks for 6-75 years with more time needed (max 4*)
	// *** the fourth entry is last   5-minute-chunks for all patients
	// *** test if added code is listed in minuteCodeMaps
	public static String[][] fiveMinuteChunkCodeMaps = {
		{
			"00.0010", "00.0020", "00.0025", "00.0026", "00.0030"
		}, {
			"00.0060", "00.0070", "00.0075", "00.0076", "00.0080"
		}, {
			"00.0110", "00.0120", "00.0125", "00.0126", "00.0130"
		}, {
			//"Telefonische, komplementärmedizinische Konsultation durch den Facharzt, 5 Min."
			"00.1880", "00.1890", "00.1895", "00.1896", "00.1900"
		}, {
			// "Akupunktur, Konsultation durch den Facharzt, erste 5 Min.";"00.1710"
			"00.1710", "00.1720", "00.1730"
		}, {
			// "Neuraltherapie, Konsultation durch den Facharzt, erste 5 Min.";"00.1740"
			"00.1740", "00.1750", "00.1760"
		}, {
			// "Homöopathie, Konsultation durch den Facharzt, erste 5 Min.";"00.1770"
			"00.1770", "00.1780", "00.1790"
		}, {
			// "Traditionelle Chinesische Medizin ({TCM}), Konsultation durch den Facharzt, erste 5 Min.";"00.1810"
			"00.1810", "00.1820", "00.1830"
		}, {
			// "Anthroposophische Medizin, Konsultation durch den Facharzt, erste 5 Min.";"00.1840"
			"00.1840", "00.1850", "00.1860"
		}, {
			// "Phytotherapie durch Facharzt, Konsultation durch den Facharzt, erste 5 Min.";"00.1870"
			"00.1870", "00.1871", "00.1872"
		}, {
			// "Telefonische, komplementärmedizinische Konsultation durch den Facharzt, erste 5 Min.";"00.1880"
			"00.1880", "00.1890", "00.1895", "00.1896", "00.1900"
		}
	};
	
	public static String[][] yearReplacements = {
		{
			" (Grundkonsultation)", ""
		}, {
			" (Konsultationszuschlag)", ""
		}, {
			" (Grundbesuch)", ""
		}, {
			" (Besuchszuschlag)", ""
		}, {
			" erste 5 Min.", " 5 Min."
		}, {
			" letzte 5 Min.", " 5 Min."
		}, {
			" jede weiteren 5 Min.", " 5 Min."
		}, {
			" bei Personen über 6 Jahren und unter 75 Jahren", ""
		}, {
			" bei Kindern unter 6 Jahren und Personen über 75 Jahren", ""
		}, {
			" bei Personen über 6 Jahren und unter 75 Jahren mit einem erhöhten Behandlungsbedarf",
			""
		},
	};
	
	public static String[][] ageGroupLists = {
		// *** col 1: 6-75
		// *** col 0: <6, >75
		// *** col 2: 6-75, erhöhter Aufwand
		{
			"00.0415", "00.0416", "00.0417",
			"Kleine Untersuchung durch den Facharzt für Grundversorgung"
		}, {
			"00.0435", "00.0436", "00.0437",
			"Kleine rheumatologische Untersuchung durch den Facharzt für Rheumatologie, Physikalische Medizin und Rehabilitation"
		}, {
			"00.0510", "00.0515", "00.0516",
			"Spezifische Beratung durch den Facharzt für Grundversorgung"
		}, {
			"00.0530", "00.0535", "00.0536", "Genetische u/o pränatale Beratung durch den Facharzt"
		}, {
			"00.0610", "00.0615", "00.0616",
			"Instruktion von Selbstmessungen, Selbstbehandlungen durch den Facharzt"
		}, {
			"00.1370", "00.1375", "00.1376", "Nachbetreuung/Betreuung/Überwachung in der Arztpraxis"
		}, {
			"02.0060", "02.0065", "02.0066",
			"Telefonische Konsultation durch den Facharzt für Psychiatrie"
		}, {
			"02.0150", "02.0155", "02.0156",
			"Telefonische Konsultation durch behandelnden Psychologen/Psychotherapeuten"
		}, {
			"04.0015", "04.0016", "04.0017", "Untersuchung durch den Facharzt für Dermatologie"
		}, {
			"00.0050", "00.0055", "00.0056",
			"Vorbesprechung diagnostischer/therapeutischer Eingriffe mit Patienten/Angehörigen durch den Facharzt"
		}, {
			"00.0141", "00.0131", "00.0161", "Aktenstudium in Abwesenheit des Patienten"
		}, {
			"00.0142", "00.0132", "00.0162",
			"Erkundigungen bei Dritten in Abwesenheit des Patienten"
		}, {
			"00.0143", "00.0133", "00.0163",
			"Auskünfte an Angehörige oder andere Bezugspersonen des Patienten in Abwesenheit des Patienten"
		}, {
			"00.0144", "00.0134", "00.0164",
			"Besprechungen mit Therapeuten und Betreuern des Patienten in Abwesenheit des Patienten"
		}, {
			"00.0145", "00.0135", "00.0165",
			"Überweisungen an Konsiliarärzte in Abwesenheit des Patienten"
		}, {
			"00.0146", "00.0136", "00.0166",
			"Ausstellen von Rezepten oder Verordnungen ausserhalb von Konsultation, Besuch und telefonischer Konsultation in Abwesenheit des Patienten"
		}, {
			"00.0147", "00.0137", "00.0167",
			"Diagnostische Leistung am Institut für Pathologie/Histologie/Zytologie in Abwesenheit des Patienten"
		}, {
			"00.0148", "00.0138", "00.0168", "Tumorboard in Abwesenheit des Patienten"
		}, {
			"02.0060", "02.0065", "02.0066",
			"Telefonische Konsultation durch den Facharzt für Psychiatrie"
		}, {
			"02.0150", "02.0155", "02.0156",
			"Telefonische Konsultation durch behandelnden Psychologen/Psychotherapeuten"
		}
	};
	
	static String[][] minuteCodeMapsList = {
		{
			"00.0020", "00.0025", "00.0030"
		}, {
			"00.0070", "00.0075", "00.0080"
		}, {
			"00.0120", "00.0125", "00.0130"
		}
	};
	// +++++ END minutes
	
	/**
	 * Hier kann eine Konsultation als Ganzes nochmal überprüft werden
	 */
	public Result<Object> optify(Konsultation kons){
		LinkedList<TarmedLeistung> postponed = new LinkedList<TarmedLeistung>();
		for (Verrechnet vv : kons.getLeistungen()) {
			IVerrechenbar iv = vv.getVerrechenbar();
			if (iv instanceof TarmedLeistung) {
				TarmedLeistung tl = (TarmedLeistung) iv;
				String tcid = tl.getCode();
				if ((tcid.equals("35.0020")) || (tcid.equals("04.1930")) //$NON-NLS-1$ //$NON-NLS-2$
					|| tcid.startsWith("00.25")) { //$NON-NLS-1$
					postponed.add(tl);
				}
			}
		}
		return null;
	}
	
	/**
	 * Eine Verrechnungsposition zufügen. Der Optifier muss prüfen, ob die Verrechnungsposition im
	 * Kontext der übergebenen Konsultation verwendet werden kann und kann sie ggf. zurückweisen
	 * oder modifizieren.
	 */
	
	public Result<IVerrechenbar> add(IVerrechenbar code, Konsultation kons){
		//		if (optifierDisabled)
		//			return new Result<IVerrechenbar>(Result.SEVERITY.OK, PREISAENDERUNG, "Preis", null, //$NON-NLS-1$
		//				false);
		
		if (!(code instanceof TarmedLeistung)) {
			return new Result<IVerrechenbar>(Result.SEVERITY.ERROR, LEISTUNGSTYP,
				Messages.TarmedOptifier_BadType, null, true);
		}
		
		bOptify = CoreHub.userCfg.get(Preferences.LEISTUNGSCODES_OPTIFY, true);
		
		TarmedLeistung tc = (TarmedLeistung) code;
		List<Verrechnet> lst = kons.getLeistungen();
		/*
		 * TODO Hier checken, ob dieser code mit der Dignität und
		 * Fachspezialisierung des aktuellen Mandanten usw. vereinbar ist
		 */
		
		Hashtable<String, String> ext = tc.loadExtension();
		
		// +++++ START minutes
		String tcid = code.getCode();
		doMinuteOptify = true;
		if (doMinuteOptify) {
			String law = kons.getFall().getRequiredString("Gesetz");
			TimeTool date = new TimeTool(kons.getDatum());
			
			// ****************************************
			// *** age group "redirects"
			String joinedAgeMap = "";
			int ageGroupIx = -1;
			for (ageGroupIx = 0; ageGroupIx < ageGroupLists.length; ageGroupIx++) {
				String[] ageMap = ageGroupLists[ageGroupIx];
				joinedAgeMap = "," + StringTool.join(ageMap, ",") + ",";
				if (joinedAgeMap.contains("," + tcid + ",")) {
					// ***  test if current kons is for < 6 years or > 75 years
					boolean isChildOrOlder = false; // *** default (NOT KVG)
					if (law.equalsIgnoreCase("KVG")) {
						IVerrechenbar childrensVerrechenbar =
							TarmedLeistung.getFromCode(ageMap[0], date, law);
						TarmedLeistung tc2 = (TarmedLeistung) childrensVerrechenbar;
						Hashtable<String, String> extChildren = tc2.loadExtension();
						String ageLimits = extChildren.get(TarmedLeistung.EXT_FLD_SERVICE_AGE);
						isChildOrOlder = true;
						if (ageLimits != null && !ageLimits.isEmpty()) {
							String errorMessage = checkAge(ageLimits, kons);
							if (errorMessage != null)
								isChildOrOlder = false;
						}
					}
					String newCode = tcid;
					if (isChildOrOlder) {
						newCode = ageMap[0];
						
					} else {
						newCode = ageMap[1];
					}
					// if the code has been changed, then recall proc with
					// changed code and return
					if (!tcid.equals(newCode)) {
						IVerrechenbar toBeAdded = TarmedLeistung.getFromCode(newCode, date, law);
						return kons.addLeistung(toBeAdded);
					}
					break;
				}
			}
		}
		// +++++ END minutes
		
		// Gültigkeit gemäss Datum und Alter prüfen
		if (bOptify) {
			TimeTool date = new TimeTool(kons.getDatum());
			String dVon = ((TarmedLeistung) code).get("GueltigVon"); //$NON-NLS-1$
			if (!StringTool.isNothing(dVon)) {
				TimeTool tVon = new TimeTool(dVon);
				if (date.isBefore(tVon)) {
					return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, NOTYETVALID,
						code.getCode() + Messages.TarmedOptifier_NotYetValid, null, false);
				}
			}
			String dBis = ((TarmedLeistung) code).get("GueltigBis"); //$NON-NLS-1$
			if (!StringTool.isNothing(dBis)) {
				TimeTool tBis = new TimeTool(dBis);
				if (date.isAfter(tBis)) {
					return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, NOMOREVALID,
						code.getCode() + Messages.TarmedOptifier_NoMoreValid, null, false);
				}
			}
			String ageLimits = ext.get(TarmedLeistung.EXT_FLD_SERVICE_AGE);
			if (ageLimits != null && !ageLimits.isEmpty()) {
				String errorMessage = checkAge(ageLimits, kons);
				if (errorMessage != null) {
					return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, PATIENTAGE,
						errorMessage, null, false);
				}
			}
		}
		newVerrechnet = null;
		newVerrechnetSide = null;
		// Korrekter Fall Typ prüfen, und ggf. den code ändern
		if (tc.getCode().matches("39.002[01]") || tc.getCode().matches("39.001[0156]")) {
			String gesetz = kons.getFall().getRequiredString("Gesetz");
			if (gesetz == null || gesetz.isEmpty()) {
				gesetz = kons.getFall().getAbrechnungsSystem();
			}
			
			if (gesetz.equalsIgnoreCase("KVG") && tc.getCode().matches("39.0011")) {
				return this.add(getKonsVerrechenbar("39.0010", kons), kons);
			} else if (!gesetz.equalsIgnoreCase("KVG") && tc.getCode().matches("39.0010")) {
				return this.add(getKonsVerrechenbar("39.0011", kons), kons);
			}
			
			if (gesetz.equalsIgnoreCase("KVG") && tc.getCode().matches("39.0016")) {
				return this.add(getKonsVerrechenbar("39.0015", kons), kons);
			} else if (!gesetz.equalsIgnoreCase("KVG") && tc.getCode().matches("39.0015")) {
				return this.add(getKonsVerrechenbar("39.0016", kons), kons);
			}
			
			if (gesetz.equalsIgnoreCase("KVG") && tc.getCode().matches("39.0021")) {
				return this.add(getKonsVerrechenbar("39.0020", kons), kons);
			} else if (!gesetz.equalsIgnoreCase("KVG") && tc.getCode().matches("39.0020")) {
				return this.add(getKonsVerrechenbar("39.0021", kons), kons);
			}
		}
		
		if (tc.getCode().matches("35.0020")) {
			List<Verrechnet> opCodes = getOPList(lst);
			List<Verrechnet> opReduction = getVerrechnetMatchingCode(lst, "35.0020");
			// updated reductions to codes, and get not yet reduced codes
			List<Verrechnet> availableCodes = updateOPReductions(opCodes, opReduction);
			if (availableCodes.isEmpty()) {
				return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, KOMBINATION,
					code.getCode(), null, false);
			}
			for (Verrechnet verrechnet : availableCodes) {
				newVerrechnet = new Verrechnet(tc, kons, 1);
				mapOpReduction(verrechnet, newVerrechnet);
			}
			return new Result<IVerrechenbar>(null);
		}
		
		// +++++ START minutes
		if (doMinuteOptify) {
			String law = kons.getFall().getRequiredString("Gesetz");
			TimeTool date = new TimeTool(kons.getDatum());
			
			// +++++ loop
			boolean skip = false;
			if (tcid.equalsIgnoreCase("00.0020"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0025"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0026"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0030"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0070"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0075"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0076"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0080"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0120"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0125"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0126"))
				skip = true;
			if (tcid.equalsIgnoreCase("00.0130"))
				skip = true;
			if (!skip) {
				// special handling for:
				// Konsultation: 00.0010, 00.0020, 00.25, 00.0030
				// Besuch: 00.0060, 00.0070, 00.0080
				// Tel Kons: 00.0110, 00.0120, 00.0130
				// *** count existing 5 minute chunks
				// *** get age group
				// *** if age under 6/over 75: add add up to 30 minutes
				// *** if age over 6/under 75: add add up to 20 minutes
				// *** if age over 6/under 75: add add up to 30 minutes after telling the user that it is necessary to...
				int codeMapIx = -1;
				String joinedCodeMap = "";
				for (codeMapIx = 0; codeMapIx < fiveMinuteChunkCodeMaps.length; codeMapIx++) {
					String[] codeMap = fiveMinuteChunkCodeMaps[codeMapIx];
					joinedCodeMap = "," + StringTool.join(codeMap, ",") + ",";
					if (joinedCodeMap.contains("," + tcid + ",")) {
						// *** found a match
						// *** count existing # of 5-minute-chunks
						int numberOfFiveMinuteChunks = 0;
						for (Verrechnet v : lst) {
							String theCode = v.getCode();
							for (int iii = 0; iii < codeMap.length; iii++) {
								if (theCode.equalsIgnoreCase(codeMap[iii])) {
									numberOfFiveMinuteChunks =
										numberOfFiveMinuteChunks + v.getZahl();
									break;
								}
							}
						}
						
						// ***  test if current kons is for < 6 years or > 75 years
						boolean isChildOrOlder = false; // *** default (NOT KVG)
						if (law.equalsIgnoreCase("KVG")) {
							IVerrechenbar childrensVerrechenbar =
								TarmedLeistung.getFromCode(codeMap[2], date, law);
							TarmedLeistung tc2 = (TarmedLeistung) childrensVerrechenbar;
							Hashtable<String, String> extChildren = tc2.loadExtension();
							String ageLimits = extChildren.get(TarmedLeistung.EXT_FLD_SERVICE_AGE);
							isChildOrOlder = true;
							if (ageLimits != null && !ageLimits.isEmpty()) {
								String errorMessage = checkAge(ageLimits, kons);
								if (errorMessage != null)
									isChildOrOlder = false;
							}
						}
						
						// *** 
						String newCode = tcid;
						switch (numberOfFiveMinuteChunks) {
						case 0:
							newCode = codeMap[0];
							break;
						case 1:
							newCode = codeMap[4];
							break;
						case 2:
						case 3:
							if (isChildOrOlder)
								newCode = codeMap[2];
							else
								newCode = codeMap[1];
							break;
						case 4:
						case 5:
							if (isChildOrOlder)
								newCode = codeMap[2];
							else {
								if (law.equalsIgnoreCase("KVG")) {
									boolean okClicked = SWTHelper.askYesNo("Verrechnung",
										"Achtung: über 20 Minuten mit spezieller Notiz in der KG/Begründung für die KK");
									if (okClicked) {
										IVerrechenbar toBeAdded =
											TarmedLeistung.getFromCode(codeMap[3], date, law);
										// *** must remove existing 00.0xx0 and replace by 00.0xx6
										if (numberOfFiveMinuteChunks == 4) {
											for (Verrechnet v : lst) {
												String theCode = v.getCode();
												if (theCode.equalsIgnoreCase(codeMap[1]))
													kons.removeLeistung(v);
											}
											kons.addLeistung(toBeAdded);
											kons.addLeistung(toBeAdded);
										}
										// *** add new 00.0xx6 version
										return kons.addLeistung(toBeAdded);
									} else {
										return new Result<IVerrechenbar>(null);
									}
								} else {
									return new Result<IVerrechenbar>(Result.SEVERITY.WARNING,
										EXKLUSION, "Maximal 20 Minuten abrechenbar.", //$NON-NLS-1$
										null, false);
								}
							}
							break;
						case 6:
							return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSION,
								"mehr als 30 geht grunzipiell nicht.", //$NON-NLS-1$
								null, false);
						}
						
						// if the code has been changed, then recall proc with
						// changed code and return
						if (!tcid.equals(newCode)) {
							IVerrechenbar toBeAdded =
								TarmedLeistung.getFromCode(newCode, date, law);
							return kons.addLeistung(toBeAdded);
						}
						break;
					}
				}
			}
		}
		// +++++ END minutes
		
		// Ist der Hinzuzufügende Code vielleicht schon in der Liste? Dann
		// nur Zahl erhöhen.
		for (Verrechnet v : lst) {
			if (v.isInstance(code)) {
				if (!tc.requiresSide()) {
					newVerrechnet = v;
					newVerrechnet.setZahl(newVerrechnet.getZahl() + 1);
					break;
				}
			}
		}
		
		if (tc.requiresSide()) {
			newVerrechnetSide = getNewVerrechnetSideOrIncrement(code, lst);
		}
		
		// Ausschliessende Kriterien prüfen ("Nicht zusammen mit")
		if (newVerrechnet == null) {
			newVerrechnet = new Verrechnet(code, kons, 1);
			// make sure side is initialized
			if (tc.requiresSide()) {
				newVerrechnet.setDetail(TarmedLeistung.SIDE, newVerrechnetSide);
			}
			// Exclusionen
			if (bOptify) {
				TarmedLeistung newTarmed = (TarmedLeistung) code;
				for (Verrechnet v : lst) {
					if (v.getVerrechenbar() instanceof TarmedLeistung) {
						TarmedLeistung tarmed = (TarmedLeistung) v.getVerrechenbar();
						if (tarmed != null && tarmed.exists()) {
							// check if new has an exclusion for this verrechnet
							// tarmed
							Result<IVerrechenbar> resCompatible =
								isCompatible(tarmed, newTarmed, kons);
							if (resCompatible.isOK()) {
								// check if existing tarmed has exclusion for
								// new one
								resCompatible = isCompatible(newTarmed, tarmed, kons);
							}
							
							if (!resCompatible.isOK()) {
								newVerrechnet.delete();
								return resCompatible;
							}
						}
					}
				}
				
				if (newVerrechnet.getCode().equals("00.0750")
					|| newVerrechnet.getCode().equals("00.0010")) {
					String excludeCode = null;
					if (newVerrechnet.getCode().equals("00.0010")) {
						excludeCode = "00.0750";
					} else {
						excludeCode = "00.0010";
					}
					for (Verrechnet v : lst) {
						if (v.getCode().equals(excludeCode)) {
							newVerrechnet.delete();
							return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSION,
								"00.0750 ist nicht im Rahmen einer ärztlichen Beratung 00.0010 verrechnenbar.", //$NON-NLS-1$
								null, false);
						}
					}
				}
			}
			newVerrechnet.setDetail(AL, Integer.toString(tc.getAL(kons.getMandant())));
			newVerrechnet.setDetail(TL, Integer.toString(tc.getTL()));
			lst.add(newVerrechnet);
		}
		
		// set bezug of zuschlagsleistung and referenzleistung
		if (isReferenceInfoAvailable() && shouldDetermineReference(tc)) {
			// lookup available masters
			List<Verrechnet> masters = getPossibleMasters(newVerrechnet, lst);
			if (masters.isEmpty()) {
				int zahl = newVerrechnet.getZahl();
				if (zahl > 1) {
					newVerrechnet.setZahl(zahl - 1);
				} else {
					newVerrechnet.delete();
				}
				return new Result<IVerrechenbar>(
					Result.SEVERITY.WARNING, KOMBINATION, "Für die Zuschlagsleistung "
						+ code.getCode() + " konnte keine passende Hauptleistung gefunden werden.",
					null, false);
			}
			if (!masters.isEmpty()) {
				String bezug = newVerrechnet.getDetail("Bezug");
				if (bezug == null) {
					// set bezug to first available master
					newVerrechnet.setDetail("Bezug", masters.get(0).getCode());
				} else {
					boolean found = false;
					// lookup matching, or create new Verrechnet
					for (Verrechnet mVerr : masters) {
						if (mVerr.getCode().equals(bezug)) {
							// just mark as found as amount is already increased
							found = true;
						}
					}
					if (!found) {
						// create a new Verrechent and decrease amount
						newVerrechnet.setZahl(newVerrechnet.getZahl() - 1);
						newVerrechnet = new Verrechnet(code, kons, 1);
						newVerrechnet.setDetail("Bezug", masters.get(0).getCode());
					}
				}
			}
		}
		
		Result<IVerrechenbar> limitResult = checkLimitations(kons, tc, newVerrechnet);
		if (!limitResult.isOK()) {
			return limitResult;
		}
		
		// +++++ START minutes
		// String tcid = code.getCode();
		// +++++ END minutes
		
		// check if it's an X-RAY service and add default tax if so
		// default xray tax will only be added once (see above)
		if (!tc.getCode().equals(DEFAULT_TAX_XRAY_ROOM) && !tc.getCode().matches("39.002[01]")
			&& tc.getParent().startsWith(CHAPTER_XRAY)) {
			if (CoreHub.userCfg.get(Preferences.LEISTUNGSCODES_OPTIFY_XRAY, true)) {
				add(getKonsVerrechenbar(DEFAULT_TAX_XRAY_ROOM, kons), kons);
				// add 39.0020, will be changed according to case (see above)
				add(getKonsVerrechenbar("39.0020", kons), kons);
			}
		}
		
		// Interventionelle Schmerztherapie: Zuschlag cervical und thoracal
		else if (tcid.equals("29.2090")) {
			double sumAL = 0.0;
			double sumTL = 0.0;
			for (Verrechnet v : lst) {
				if (v.getVerrechenbar() instanceof TarmedLeistung) {
					TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
					String tlc = tl.getCode();
					double z = v.getZahl();
					if (tlc.matches("29.20[12345678]0") || (tlc.equals("29.2200"))) {
						sumAL += (z * tl.getAL(kons.getMandant())) / 2;
						sumTL += (z * tl.getTL()) / 4;
					}
				}
			}
			newVerrechnet.setTP(sumAL + sumTL);
			newVerrechnet.setDetail(AL, Double.toString(sumAL));
			newVerrechnet.setDetail(TL, Double.toString(sumTL));
		}
		
		// Zuschlag Kinder
		else if (tcid.equals("00.0010") || tcid.equals("00.0060")) {
			if (CoreHub.mandantCfg != null
				&& CoreHub.mandantCfg.get(RechnungsPrefs.PREF_ADDCHILDREN, false)) {
				Fall f = kons.getFall();
				if (f != null) {
					Patient p = f.getPatient();
					if (p != null) {
						String alter = p.getAlter();
						if (Integer.parseInt(alter) < 6) {
							TarmedLeistung tl =
								(TarmedLeistung) getKonsVerrechenbar("00.0040", kons);
							add(tl, kons);
						}
					}
				}
			}
		}
		
		// Zuschläge für Insellappen 50% auf AL und TL bei 1910,20,40,50
		else if (tcid.equals("04.1930")) { //$NON-NLS-1$
			double sumAL = 0.0;
			double sumTL = 0.0;
			for (Verrechnet v : lst) {
				if (v.getVerrechenbar() instanceof TarmedLeistung) {
					TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
					String tlc = tl.getCode();
					int z = v.getZahl();
					if (tlc.equals("04.1910") || tlc.equals("04.1920") || tlc.equals("04.1940") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						|| tlc.equals("04.1950")) { //$NON-NLS-1$
						sumAL += tl.getAL(kons.getMandant()) * z;
						sumTL += tl.getTL() * z;
						// double al = (tl.getAL() * 15) / 10.0;
						// double tel = (tl.getTL() * 15) / 10.0;
						// sum += al * z;
						// sum += tel * z;
					}
				}
			}
			// sum = sum * factor / 100.0;
			// check.setPreis(new Money(sum));
			newVerrechnet.setTP(sumAL + sumTL);
			newVerrechnet.setDetail(AL, Double.toString(sumAL));
			newVerrechnet.setDetail(TL, Double.toString(sumTL));
			newVerrechnet.setPrimaryScaleFactor(0.5);
		}
		// Zuschläge für 04.0620 sollte sich diese mit 70% auf die Positionen 04.0630 & 04.0640 beziehen
		else if (tcid.equals("04.0620")) {
			double sumAL = 0.0;
			double sumTL = 0.0;
			for (Verrechnet v : lst) {
				if (v.getVerrechenbar() instanceof TarmedLeistung) {
					TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
					String tlc = tl.getCode();
					int z = v.getZahl();
					if (tlc.equals("04.0610") || tlc.equals("04.0630") || tlc.equals("04.0640")) {
						sumAL += tl.getAL(kons.getMandant()) * z;
						sumTL += tl.getTL() * z;
					}
				}
			}
			newVerrechnet.setTP(sumAL + sumTL);
			newVerrechnet.setDetail(AL, Double.toString(sumAL));
			newVerrechnet.setDetail(TL, Double.toString(sumTL));
			newVerrechnet.setPrimaryScaleFactor(0.7);
		}
		
		// Notfall-Zuschläge
		if (tcid.startsWith("00.25")) { //$NON-NLS-1$
			double sum = 0.0;
			int subcode = Integer.parseInt(tcid.substring(5));
			switch (subcode) {
			case 10: // Mo-Fr 7-19, Sa 7-12: 60 TP
				break;
			case 20: // Mo-Fr 19-22, Sa 12-22, So 7-22: 120 TP
				break;
			case 30: // 25% zu allen AL von 20
			case 70: // 25% zu allen AL von 60 (tel.)
				for (Verrechnet v : lst) {
					if (v.getVerrechenbar() instanceof TarmedLeistung) {
						TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
						if (tl.getCode().startsWith("00.25")) { //$NON-NLS-1$
							continue;
						}
						sum += (tl.getAL(kons.getMandant()) * v.getZahl());
						// int summand = tl.getAL() >> 2; // TODO ev. float?
						// -> Rundung?
						// ((sum.addCent(summand * v.getZahl());
					}
				}
				// check.setPreis(sum.multiply(factor));
				newVerrechnet.setTP(sum);
				newVerrechnet.setDetail(AL, Double.toString(sum));
				newVerrechnet.setPrimaryScaleFactor(0.25);
				break;
			case 40: // 22-7: 180 TP
				break;
			case 50: // 50% zu allen AL von 40
			case 90: // 50% zu allen AL von 70 (tel.)
				for (Verrechnet v : lst) {
					if (v.getVerrechenbar() instanceof TarmedLeistung) {
						TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
						if (tl.getCode().startsWith("00.25")) { //$NON-NLS-1$
							continue;
						}
						// int summand = tl.getAL() >> 1;
						// sum.addCent(summand * v.getZahl());
						sum += (tl.getAL(kons.getMandant()) * v.getZahl());
					}
				}
				// check.setPreis(sum.multiply(factor));
				newVerrechnet.setTP(sum);
				newVerrechnet.setDetail(AL, Double.toString(sum));
				newVerrechnet.setPrimaryScaleFactor(0.5);
				break;
			
			case 60: // Tel. Mo-Fr 19-22, Sa 12-22, So 7-22: 30 TP
				break;
			case 80: // Tel. von 22-7: 70 TP
				break;
			
			}
			return new Result<IVerrechenbar>(Result.SEVERITY.OK, PREISAENDERUNG, "Preis", null, //$NON-NLS-1$
				false);
		}
		return new Result<IVerrechenbar>(null);
	}
	
	private Result<IVerrechenbar> checkLimitations(Konsultation kons, TarmedLeistung tarmedLeistung,
		Verrechnet newVerrechnet){
		if (bOptify) {
			// service limitations
			List<TarmedLimitation> limitations = tarmedLeistung.getLimitations();
			for (TarmedLimitation tarmedLimitation : limitations) {
				if (tarmedLimitation.isTestable()) {
					Result<IVerrechenbar> result = tarmedLimitation.test(kons, newVerrechnet);
					if (!result.isOK()) {
						return result;
					}
				}
			}
			// group limitations
			TimeTool date = new TimeTool(kons.getDatum());
			List<String> groups = tarmedLeistung.getServiceGroups(date);
			for (String groupName : groups) {
				Optional<TarmedGroup> group =
					TarmedGroup.find(groupName, tarmedLeistung.get(TarmedLeistung.FLD_LAW), date);
				if (group.isPresent()) {
					limitations = group.get().getLimitations();
					for (TarmedLimitation tarmedLimitation : limitations) {
						if (tarmedLimitation.isTestable()) {
							Result<IVerrechenbar> result =
								tarmedLimitation.test(kons, newVerrechnet);
							if (!result.isOK()) {
								return result;
							}
						}
					}
				}
			}
		}
		return new Result<IVerrechenbar>(null);
	}
	
	private String checkAge(String limitsString, Konsultation kons){
		LocalDateTime consDate = new TimeTool(kons.getDatum()).toLocalDateTime();
		Patient patient = kons.getFall().getPatient();
		String geburtsdatum = patient.getGeburtsdatum();
		if (StringUtils.isEmpty(geburtsdatum)) {
			return "Patienten Alter nicht ok, kein Geburtsdatum angegeben";
		}
		long patientAgeDays = patient.getAgeAt(consDate, ChronoUnit.DAYS);
		
		List<TarmedLeistungAge> ageLimits = TarmedLeistungAge.of(limitsString, consDate);
		for (TarmedLeistungAge tarmedLeistungAge : ageLimits) {
			if (tarmedLeistungAge.isValidOn(consDate.toLocalDate())) {
				// if only one of the limits is set, check only that limit
				if (tarmedLeistungAge.getFromDays() >= 0 && !(tarmedLeistungAge.getToDays() >= 0)) {
					if (patientAgeDays < tarmedLeistungAge.getFromDays()) {
						return "Patient ist zu jung, verrechenbar ab "
							+ tarmedLeistungAge.getFromText();
					}
				} else if (tarmedLeistungAge.getToDays() >= 0
					&& !(tarmedLeistungAge.getFromDays() >= 0)) {
					if (patientAgeDays > tarmedLeistungAge.getToDays()) {
						return "Patient ist zu alt, verrechenbar bis "
							+ tarmedLeistungAge.getToText();
					}
				} else if (tarmedLeistungAge.getToDays() >= 0
					&& tarmedLeistungAge.getFromDays() >= 0) {
					if (tarmedLeistungAge.getToDays() < tarmedLeistungAge.getFromDays()) {
						if (patientAgeDays > tarmedLeistungAge.getToDays()
							&& patientAgeDays < tarmedLeistungAge.getFromDays()) {
							return "Patienten Alter nicht ok, verrechenbar "
								+ tarmedLeistungAge.getText();
						}
					} else {
						if (patientAgeDays > tarmedLeistungAge.getToDays()
							|| patientAgeDays < tarmedLeistungAge.getFromDays()) {
							return "Patienten Alter nicht ok, verrechenbar "
								+ tarmedLeistungAge.getText();
						}
					}
				}
			}
		}
		return null;
	}
	
	private IVerrechenbar getKonsVerrechenbar(String code, Konsultation kons){
		TimeTool date = new TimeTool(kons.getDatum());
		if (kons.getFall() != null) {
			String law = kons.getFall().getRequiredString("Gesetz");
			return TarmedLeistung.getFromCode(code, date, law);
		}
		return null;
	}
	
	private boolean isReferenceInfoAvailable(){
		return CoreHub.globalCfg.get(TarmedReferenceDataImporter.CFG_REFERENCEINFO_AVAILABLE,
			false);
	}
	
	private boolean shouldDetermineReference(TarmedLeistung tc){
		String typ = tc.getServiceTyp();
		boolean becauseOfType = typ.equals("Z");
		if (becauseOfType) {
			String text = tc.getText();
			return text.startsWith("+") || text.startsWith("-");
		}
		return false;
	}
	
	private List<Verrechnet> getAvailableMasters(TarmedLeistung slave, List<Verrechnet> lst){
		List<Verrechnet> ret = new LinkedList<Verrechnet>();
		TimeTool konsDate = null;
		for (Verrechnet v : lst) {
			if (konsDate == null) {
				konsDate = new TimeTool(v.getKons().getDatum());
			}
			if (v.getVerrechenbar() instanceof TarmedLeistung) {
				TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
				if (tl.getHierarchy(konsDate).contains(slave.getCode())) { //$NON-NLS-1$
					ret.add(v);
				}
			}
		}
		return ret;
	}
	
	private List<Verrechnet> getPossibleMasters(Verrechnet newSlave, List<Verrechnet> lst){
		TarmedLeistung slaveTarmed = (TarmedLeistung) newSlave.getVerrechenbar();
		// lookup available masters
		List<Verrechnet> masters = getAvailableMasters(slaveTarmed, lst);
		// check which masters are left to be referenced
		int maxPerMaster = getMaxPerMaster(slaveTarmed);
		if (maxPerMaster > 0) {
			Map<Verrechnet, List<Verrechnet>> masterSlavesMap = getMasterToSlavesMap(newSlave, lst);
			for (Verrechnet master : masterSlavesMap.keySet()) {
				int masterCount = master.getZahl();
				int slaveCount = 0;
				for (Verrechnet slave : masterSlavesMap.get(master)) {
					slaveCount += slave.getZahl();
					if (slave.equals(newSlave)) {
						slaveCount--;
					}
				}
				if (masterCount <= (slaveCount * maxPerMaster)) {
					masters.remove(master);
				}
			}
		}
		return masters;
	}
	
	/**
	 * Creates a map of masters associated to slaves by the Bezug. This map will not contain the
	 * newSlave, as it has no Bezug set yet.
	 * 
	 * @param newSlave
	 * @param lst
	 * @return
	 */
	private Map<Verrechnet, List<Verrechnet>> getMasterToSlavesMap(Verrechnet newSlave,
		List<Verrechnet> lst){
		Map<Verrechnet, List<Verrechnet>> ret = new HashMap<>();
		TarmedLeistung slaveTarmed = (TarmedLeistung) newSlave.getVerrechenbar();
		// lookup available masters
		List<Verrechnet> masters = getAvailableMasters(slaveTarmed, lst);
		for (Verrechnet verrechnet : masters) {
			ret.put(verrechnet, new ArrayList<Verrechnet>());
		}
		// lookup other slaves with same code
		List<Verrechnet> slaves = getVerrechnetMatchingCode(lst, newSlave.getCode());
		// add slaves to separate master list
		for (Verrechnet slave : slaves) {
			String bezug = slave.getDetail("Bezug");
			if (bezug != null && !bezug.isEmpty()) {
				for (Verrechnet master : ret.keySet()) {
					if (master.getCode().equals(bezug)) {
						ret.get(master).add(slave);
					}
				}
			}
		}
		return ret;
	}
	
	private int getMaxPerMaster(TarmedLeistung slave){
		List<TarmedLimitation> limits = slave.getLimitations();
		for (TarmedLimitation limit : limits) {
			if (limit.getLimitationUnit() == LimitationUnit.MAINSERVICE) {
				// only an integer makes sense here
				return (int) limit.getAmount();
			}
		}
		// default to unknown
		return -1;
	}
	
	/**
	 * Create a new mapping between an OP I reduction (35.0020) and a service from the OP I section.
	 * 
	 * @param opVerrechnet
	 *            Verrechnet representing a service from the OP I section
	 * @param reductionVerrechnet
	 *            Verrechnet representing the OP I reduction (35.0020)
	 */
	private void mapOpReduction(Verrechnet opVerrechnet, Verrechnet reductionVerrechnet){
		TarmedLeistung opVerrechenbar = (TarmedLeistung) opVerrechnet.getVerrechenbar();
		reductionVerrechnet.setZahl(opVerrechnet.getZahl());
		reductionVerrechnet.setDetail(TL, Double.toString(opVerrechenbar.getTL()));
		reductionVerrechnet.setDetail(AL, Double.toString(0.0));
		reductionVerrechnet.setTP(opVerrechenbar.getTL());
		reductionVerrechnet.setPrimaryScaleFactor(-0.4);
		reductionVerrechnet.setDetail("Bezug", opVerrechenbar.getCode());
	}
	
	/**
	 * Update existing OP I reductions (35.0020), and return a list of all not yet mapped OP I
	 * services.
	 * 
	 * @param opCodes
	 *            list of all available OP I codes see {@link #getOPList(List)}
	 * @param opReduction
	 *            list of all available reduction codes see {@link #getVerrechnetMatchingCode(List)}
	 * @return list of not unmapped OP I codes
	 */
	private List<Verrechnet> updateOPReductions(List<Verrechnet> opCodes,
		List<Verrechnet> opReduction){
		List<Verrechnet> notMappedCodes = new ArrayList<Verrechnet>();
		notMappedCodes.addAll(opCodes);
		// update already mapped
		for (Verrechnet reductionVerrechnet : opReduction) {
			boolean isMapped = false;
			String bezug = reductionVerrechnet.getDetail("Bezug");
			if (bezug != null && !bezug.isEmpty()) {
				for (Verrechnet opVerrechnet : opCodes) {
					TarmedLeistung opVerrechenbar = (TarmedLeistung) opVerrechnet.getVerrechenbar();
					String opCodeString = opVerrechenbar.getCode();
					if (bezug.equals(opCodeString)) {
						// update
						reductionVerrechnet.setZahl(opVerrechnet.getZahl());
						reductionVerrechnet.setDetail(TL, Double.toString(opVerrechenbar.getTL()));
						reductionVerrechnet.setDetail(AL, Double.toString(0.0));
						reductionVerrechnet.setPrimaryScaleFactor(-0.4);
						notMappedCodes.remove(opVerrechnet);
						isMapped = true;
						break;
					}
				}
			}
			if (!isMapped) {
				reductionVerrechnet.setZahl(0);
				reductionVerrechnet.setDetail("Bezug", "");
			}
		}
		
		return notMappedCodes;
	}
	
	private List<Verrechnet> getOPList(List<Verrechnet> lst){
		List<Verrechnet> ret = new ArrayList<Verrechnet>();
		for (Verrechnet v : lst) {
			if (v.getVerrechenbar() instanceof TarmedLeistung) {
				TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
				if (tl.getSparteAsText().equals("OP I")) { //$NON-NLS-1$
					ret.add(v);
				}
			}
		}
		return ret;
	}
	
	private List<Verrechnet> getVerrechnetMatchingCode(List<Verrechnet> lst, String code){
		List<Verrechnet> ret = new ArrayList<Verrechnet>();
		for (Verrechnet v : lst) {
			if (v.getVerrechenbar() instanceof TarmedLeistung) {
				TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
				if (tl.getCode().equals(code)) { //$NON-NLS-1$
					ret.add(v);
				}
			}
		}
		return ret;
	}
	
	private List<Verrechnet> getVerrechnetWithBezugMatchingCode(List<Verrechnet> lst, String code){
		List<Verrechnet> ret = new ArrayList<Verrechnet>();
		for (Verrechnet v : lst) {
			if (v.getVerrechenbar() instanceof TarmedLeistung) {
				if (code.equals(v.getDetail("Bezug"))) { //$NON-NLS-1$
					ret.add(v);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Always toggle the side of a specific code. Starts with left, then right, then add to the
	 * respective side.
	 * 
	 * @param code
	 * @param lst
	 * @return
	 */
	private String getNewVerrechnetSideOrIncrement(IVerrechenbar code, List<Verrechnet> lst){
		int countSideLeft = 0;
		Verrechnet leftVerrechnet = null;
		int countSideRight = 0;
		Verrechnet rightVerrechnet = null;
		
		for (Verrechnet v : lst) {
			if (v.isInstance(code)) {
				String side = v.getDetail(TarmedLeistung.SIDE);
				if (side.equals(TarmedLeistung.SIDE_L)) {
					countSideLeft += v.getZahl();
					leftVerrechnet = v;
				} else {
					countSideRight += v.getZahl();
					rightVerrechnet = v;
				}
			}
		}
		
		if (countSideLeft > 0 || countSideRight > 0) {
			if ((countSideLeft > countSideRight) && rightVerrechnet != null) {
				newVerrechnet = rightVerrechnet;
				newVerrechnet.setZahl(newVerrechnet.getZahl() + 1);
			} else if ((countSideLeft <= countSideRight) && leftVerrechnet != null) {
				newVerrechnet = leftVerrechnet;
				newVerrechnet.setZahl(newVerrechnet.getZahl() + 1);
			} else if ((countSideLeft > countSideRight) && rightVerrechnet == null) {
				return TarmedLeistung.SIDE_R;
			}
		}
		return TarmedLeistung.SIDE_L;
	}
	
	/**
	 * check compatibility of one tarmed with another
	 * 
	 * @param tarmedCode
	 *            the tarmed and it's parents code are check whether they have to be excluded
	 * @param tarmed
	 *            TarmedLeistung who incompatibilities are examined
	 * @param kons
	 *            {@link Konsultation} providing context
	 * @return true OK if they are compatible, WARNING if it matches an exclusion case
	 */
	public Result<IVerrechenbar> isCompatible(TarmedLeistung tarmedCode, TarmedLeistung tarmed,
		Konsultation kons){
		TimeTool date = new TimeTool(kons.getDatum());
		List<TarmedExclusion> exclusions = tarmed.getExclusions(kons);
		for (TarmedExclusion tarmedExclusion : exclusions) {
			if (tarmedExclusion.isMatching(tarmedCode, date)) {
				return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSION,
					tarmed.getCode() + " nicht kombinierbar mit " + tarmedExclusion.toString(), //$NON-NLS-1$
					null, false);
			}
		}
		List<String> groups = tarmed.getServiceGroups(date);
		for (String groupName : groups) {
			Optional<TarmedGroup> group =
				TarmedGroup.find(groupName, tarmed.get(TarmedLeistung.FLD_LAW), date);
			if (group.isPresent()) {
				List<TarmedExclusion> groupExclusions = group.get().getExclusions(kons);
				for (TarmedExclusion tarmedExclusion : groupExclusions) {
					if (tarmedExclusion.isMatching(tarmedCode, date)) {
						return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSION,
							tarmed.getCode() + " nicht kombinierbar mit " //$NON-NLS-1$
								+ tarmedExclusion.toString(),
							null, false);
					}
				}
			}
		}
		List<String> blocks = tarmed.getServiceBlocks(date);
		for (String blockName : blocks) {
			if (skipBlockExclusives(blockName)) {
				continue;
			}
			List<TarmedExclusive> exclusives = TarmedKumulation.getExclusives(blockName,
				TarmedKumulationType.BLOCK, date, tarmed.get(TarmedLeistung.FLD_LAW));
			// currently only test blocks exclusives, exclude hierarchy matches
			if (canHandleAllExculives(exclusives)
				&& !isMatchingHierarchy(tarmedCode, tarmed, date)) {
				boolean included = false;
				for (TarmedExclusive tarmedExclusive : exclusives) {
					if (tarmedExclusive.isMatching(tarmedCode, date)) {
						included = true;
					}
				}
				if (!included) {
					return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSIVE,
						tarmed.getCode() + " nicht kombinierbar mit " //$NON-NLS-1$
							+ tarmedCode.getCode() + ", wegen Block Kumulation",
						null, false);
				}
			}
		}
		return new Result<IVerrechenbar>(Result.SEVERITY.OK, OK, "compatible", null, false);
	}
	
	private boolean skipBlockExclusives(String blockName){
		try {
			Integer blockNumber = Integer.valueOf(blockName);
			if (blockNumber > 50 && blockNumber < 60) {
				return true;
			}
		} catch (NumberFormatException nfe) {
			// ignore and do not skip
		}
		return false;
	}
	
	private boolean isMatchingHierarchy(TarmedLeistung tarmedCode, TarmedLeistung tarmed,
		TimeTool date){
		return tarmed.getHierarchy(date).contains(tarmedCode.getCode());
	}
	
	/**
	 * Test if we can handle all {@link TarmedExclusive}.
	 * 
	 * @param exclusives
	 * @return
	 */
	private boolean canHandleAllExculives(List<TarmedExclusive> exclusives){
		for (TarmedExclusive tarmedExclusive : exclusives) {
			if (tarmedExclusive.getSlaveType() != TarmedKumulationType.BLOCK
				&& tarmedExclusive.getSlaveType() != TarmedKumulationType.CHAPTER
				&& tarmedExclusive.getSlaveType() != TarmedKumulationType.SERVICE) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Eine Verrechnungsposition entfernen. Der Optifier sollte prüfen, ob die Konsultation nach
	 * Entfernung dieses Codes noch konsistent verrechnet wäre und ggf. anpassen oder das Entfernen
	 * verweigern. Diese Version macht keine Prüfungen, sondern erfüllt nur die Anfrage..
	 */
	public Result<Verrechnet> remove(Verrechnet code, Konsultation kons){
		List<Verrechnet> l = kons.getLeistungen();
		l.remove(code);
		code.delete();
		// if no more left, check for bezug and remove
		List<Verrechnet> left = getVerrechnetMatchingCode(l, code.getCode());
		if (left.isEmpty()) {
			List<Verrechnet> verrechnetWithBezug =
				getVerrechnetWithBezugMatchingCode(kons.getLeistungen(), code.getCode());
			for (Verrechnet verrechnet : verrechnetWithBezug) {
				remove(verrechnet, kons);
			}
		}
		return new Result<Verrechnet>(code);
	}
	
	@Override
	public Verrechnet getCreatedVerrechnet(){
		return newVerrechnet;
	}
	
}
