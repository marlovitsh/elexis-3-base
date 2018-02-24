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

import java.text.SimpleDateFormat;
import java.time.LocalDate;

//TODO 00.0076
//TODO 00.0126

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.lang.StringUtils;

import ch.elexis.arzttarife_schweiz.Messages;
import ch.elexis.core.constants.Preferences;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.interfaces.IOptifier;
import ch.elexis.core.data.interfaces.IVerrechenbar;
import ch.elexis.core.data.interfaces.IVerrechenbar.DefaultOptifier;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.data.TarmedKumulation.TarmedKumulationType;
import ch.elexis.data.TarmedLimitation.LimitationUnit;
import ch.elexis.data.importer.TarmedLeistungAge;
import ch.elexis.data.importer.TarmedReferenceDataImporter;
import ch.elexis.tarmedprefs.RechnungsPrefs;
import ch.elexis.views.TarmedOptifierLists;
import ch.rgw.tools.JdbcLink;
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
	/**
	 * automatic optifying of 5-minute-chunks
	 */
	public static boolean doOptify5MinuteChunks = true;
	public static boolean doStripMinuteItemsFromTree = false;
	
	/**
	 * 
	 */
	public static boolean doOptifyConnectedTarmeds = true;
	
	public static boolean optifierDisabled = false;
	
	/**
	 * internal to ensure that only asked once for adding more kons
	 */
	protected Konsultation lastKonsUsed = null;
	/**
	 * internal to avoid addition lopps
	 */
	protected boolean isAddingConnected = false;
	
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
	
	// +++++ START minutes
	private Query<TarmedLeistung> childrenQuery = new Query<>(TarmedLeistung.class);
	
	/**
	 * get all the tarmed children for a given parent element for a given validity date
	 * 
	 * @param parentElement
	 *            the tarmed parent element
	 * @param date
	 *            the date for which to search the tarmed children
	 * @return TarmedLeistung[], the children found
	 */
	public TarmedLeistung[] getTarmedChildren(TarmedLeistung parentElement, TimeTool date){
		if (parentElement instanceof TarmedLeistung) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
			String strListValidFrom = simpleDateFormat.format(date.getTime());
			TarmedLeistung parentLeistung = (TarmedLeistung) parentElement;
			childrenQuery.clear();
			childrenQuery.add(TarmedLeistung.FLD_PARENT, Query.EQUALS, parentLeistung.getId());
			childrenQuery.add(TarmedLeistung.FLD_GUELTIG_VON, Query.LESS_OR_EQUAL,
				strListValidFrom);
			childrenQuery.add(TarmedLeistung.FLD_GUELTIG_BIS, Query.GREATER_OR_EQUAL,
				strListValidFrom);
			childrenQuery.orderBy(false, TarmedLeistung.FLD_CODE);
			List<TarmedLeistung> internalResult = childrenQuery.execute();
			return internalResult.toArray(new TarmedLeistung[internalResult.size()]);
		}
		return null;
	}
	
	/**
	 * Get a list of possible parents for a given child tarmed inside the current tree part. (Those
	 * items staring with "+")
	 * 
	 * @param childElement
	 * @param date
	 * @return the list of matching parent items inside the current tree part. This may be more than
	 *         one position.
	 */
	public TarmedLeistung[] getMatchingTarmedParents(TarmedLeistung childElement, TimeTool date){
		String theParent = childElement.getParent();
		TarmedLeistung tarm = TarmedLeistung.load(theParent);
		TarmedLeistung[] childrenTarmedLeistungen = getTarmedChildren(tarm, date);
		ArrayList<TarmedLeistung> matchingChildren = new ArrayList<TarmedLeistung>();
		for (TarmedLeistung tml : childrenTarmedLeistungen) {
			System.out.println(tml.getText());
			if (tml.getHierarchy(date).contains(childElement.getCode()))
				matchingChildren.add(tml);
		}
		return matchingChildren.toArray(new TarmedLeistung[matchingChildren.size()]);
	}
	
	/**
	 * test if a konsultation is inside age allowed by the limits defined for the given code
	 * 
	 * @param kons
	 *            the Konsultation for which to test
	 * @param code
	 *            the code for which to test
	 * @return true if in defined age, false if not
	 */
	public boolean isNormalAgeForCode(Konsultation kons, String code){
		TimeTool date = new TimeTool(kons.getDatum());
		String law = kons.getFall().getRequiredString("Gesetz");
		
		boolean bIsNormalAge = false; // *** default (NOT KVG)
		try {
			if (law.equalsIgnoreCase("KVG")) {
				IVerrechenbar childrensVerrechenbar = TarmedLeistung.getFromCode(code, date, law);
				TarmedLeistung tc2 = (TarmedLeistung) childrensVerrechenbar;
				Hashtable<String, String> extChildren = tc2.loadExtension();
				String ageLimits = extChildren.get(TarmedLeistung.EXT_FLD_SERVICE_AGE);
				bIsNormalAge = true;
				if (ageLimits != null && !ageLimits.isEmpty()) {
					String errorMessage = checkAge(ageLimits, kons);
					if (errorMessage != null)
						bIsNormalAge = false;
				}
			}
		} catch (Exception ex) {
			// *** no children's code defined -> bIsNormalAge = false
		}
		return bIsNormalAge;
	}
	
	public boolean isisis(Konsultation kons, String code){
		TimeTool date = new TimeTool(kons.getDatum());
		String law = kons.getFall().getRequiredString("Gesetz");
		
		boolean isChildOrOlder = false; // *** default (NOT KVG) or old version/UVG-Version
		try {
			if (law.equalsIgnoreCase("KVG")) {
				IVerrechenbar childrensVerrechenbar =
					TarmedLeistung.getFromCode(code /*codeMap[3]*/, date, law);
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
		} catch (Exception ex) {
			// *** no children's code defined -> isChildOrOlder = false
		}
		return isChildOrOlder;
	}
	// +++++ END minutes
	
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
		
		//bOptify = false;
		
		TarmedLeistung tc = (TarmedLeistung) code;
		
		// +++++ START automatic addition of needed parent codes
		boolean isKonsAfter2018 = new TimeTool(kons.getDatum()).get(Calendar.YEAR) >= 2018;
		String tcid = code.getCode();
		
		// *** handle connected tarmeds
		if (bOptify && doOptifyConnectedTarmeds) {
			if (!isAddingConnected) {
				isAddingConnected = true;
				TimeTool date = new TimeTool(kons.getDatum());
				String law = kons.getFall().getRequiredString("Gesetz");
				for (int connectedIx =
					0; connectedIx < TarmedOptifierLists.connectedTarmeds.length; connectedIx++) {
					String[] connectedMap = TarmedOptifierLists.connectedTarmeds[connectedIx];
					// *** if parent selected, then switch this to the child which causes the parent to be added, too
					if (tcid.equalsIgnoreCase(connectedMap[0])) {
						IVerrechenbar childToBeAdded =
							TarmedLeistung.getFromCode(connectedMap[1], date, law);
						code = childToBeAdded;
						tc = (TarmedLeistung) childToBeAdded;
						tcid = code.getCode();
						break;
					}
				}
			} else {
				isAddingConnected = false;
			}
		}
		
		// +++++ START minutes
		doOptify5MinuteChunks = true;
		// +++++ END minutes
		
		// +++++ START automatic addition of needed parent codes
		// *** must add before getting lst (see below)
		if (bOptify) {
			boolean isKonsultationType = false;
			for (String[] sa : TarmedOptifierLists.fiveMinuteChunkCodeMaps) {
				for (String s : sa)
					if (s.equalsIgnoreCase(tcid)) {
						isKonsultationType = true;
						break;
					}
				if (isKonsultationType)
					break;
			}
			if (!isKonsultationType) {
				TarmedLeistung[] matchingParents =
					getMatchingTarmedParents(tc, new TimeTool(kons.getDatum()));
				if (matchingParents.length > 0) {
					Result<IVerrechenbar> addResult =
						kons.addLeistung(getKonsVerrechenbar(matchingParents[0].getCode(), kons));
				}
			}
		}
		// +++++ END automatic addition of needed parent codes
		
		List<Verrechnet> lst = kons.getLeistungen();
		
		// +++++ START
		// *********************************************************************
		// *** age group "redirects" - change to correct age group, uses TarmedOptifierLists.ageGroupLists
		if (doOptify5MinuteChunks && isKonsAfter2018) {
			TimeTool date = new TimeTool(kons.getDatum());
			String law = kons.getFall().getRequiredString("Gesetz");
			
			boolean skip = TarmedOptifierLists.skipCodesArray.contains(tcid);
			if (!skip) {
				// *** test if in array TarmedOptifierLists.ageGroupLists
				for (int ageGroupIx =
					0; ageGroupIx < TarmedOptifierLists.ageGroupLists.length; ageGroupIx++) {
					String[] ageMap = TarmedOptifierLists.ageGroupLists[ageGroupIx];
					String joinedAgeMap = "," + StringTool.join(ageMap, ",") + ",";
					if (joinedAgeMap.contains("," + tcid + ",")) {
						// ***  test if current kons is for < 6 years or > 75 years
						
						// *** change to correct code for age
						String newCode = tcid;
						if (!isNormalAgeForCode(kons, ageMap[0])) {
							newCode = ageMap[1]; // *** children and older
						} else {
							String normalAgeCode = ageMap[0];
							String normalAgeCodeMore = ageMap[2];
							
							// *** this may be code for normal-age or for normal-age-with-more-time-requirement
							newCode = ageMap[0];
							
							// *** count existing # for normal-age and for normal-age-more-time-needed
							Verrechnet foundVerrechnet = null;
							Verrechnet foundVerrechnetMore = null;
							int numOfVerrechnet = 0;
							int numOfVerrechnetMore = 0;
							for (Verrechnet v : lst) {
								String theCode = v.getCode();
								if (theCode.equalsIgnoreCase(normalAgeCode)) {
									numOfVerrechnet = v.getZahl();
									foundVerrechnet = v;
								}
								if (theCode.equalsIgnoreCase(normalAgeCodeMore)) {
									numOfVerrechnetMore = v.getZahl();
									foundVerrechnetMore = v;
								}
							}
							
							if (numOfVerrechnetMore > 0) {
								newCode = ageMap[2];
								foundVerrechnet = foundVerrechnetMore;
								numOfVerrechnet = numOfVerrechnetMore;
							}
							
							// *** fake a new verrechnet to check for limitations (must remove afterwards)
							IVerrechenbar toBeAdded =
								TarmedLeistung.getFromCode(newCode, date, law);
							TarmedLeistung tc2 = (TarmedLeistung) toBeAdded;
							boolean canAdd = true;
							String limitResultMessage = "";
							if (numOfVerrechnet > 0) {
								foundVerrechnet.setZahl(foundVerrechnet.getZahl() + 1);
								Result<IVerrechenbar> limitResult =
									checkLimitations(kons, tc2, foundVerrechnet);
								foundVerrechnet.setZahl(foundVerrechnet.getZahl() - 1);
								if (!limitResult.isOK()) {
									canAdd = false;
									limitResultMessage = limitResult.getMessages().get(0).getText();
								}
							}
							if (!canAdd) {
								boolean okToAdd = true;
								if (numOfVerrechnetMore == 0)
									okToAdd = SWTHelper.askYesNo("Verrechnung", "Reguläre Limite: "
										+ limitResultMessage + "\n\n"
										+ "Wenn Sie mehr dieser Position verrechnen wollen, müssen Sie die entsprechende Tarmed-Position für vermehrten Aufwand verrechen.\n"
										+ "Wollen Sie das?\n\n(Die Gründe für den erhöhten Behandlungsbedarf eines Patienten müssen in der Patientenakte aufgeführt werden. Der erhöhte Behandlungsbedarf eines Patienten ist gegenüber dem Versicherer zu begründen)");
								//return limitResult;
								if (okToAdd) {
									// *** must remove regular normal-age-positions by increased-time-requirement-positions
									for (Verrechnet v : lst) {
										String theCode = v.getCode();
										if (theCode.equalsIgnoreCase(newCode))
											this.remove(v, kons);
									}
									// *** must re-read lst
									lst = kons.getLeistungen();
									
									// *** switch to increased-time-requirement
									newCode = ageMap[2];
									IVerrechenbar toBeAddedSwitched =
										TarmedLeistung.getFromCode(newCode, date, law);
									for (int i = 0; i < numOfVerrechnet; i++)
										kons.addLeistung(toBeAddedSwitched);
									return kons.addLeistung(toBeAddedSwitched);
								}
							}
						}
						// *** redirect by setting new code/tc/tcid
						if (!tcid.equals(newCode)) {
							IVerrechenbar toBeAdded =
								TarmedLeistung.getFromCode(newCode, date, law);
							code = toBeAdded;
							tc = (TarmedLeistung) toBeAdded;
							tcid = code.getCode();
							return kons.addLeistung(toBeAdded);
						}
						break;
					}
				}
			}
		}
		// +++++ END
		
		/*
		 * TODO Hier checken, ob dieser code mit der Dignität und
		 * Fachspezialisierung des aktuellen Mandanten usw. vereinbar ist
		 */
		
		Hashtable<String, String> ext = tc.loadExtension();
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
		// if (doOptify5MinuteChunks && isAfter2018) {
		if (doOptify5MinuteChunks) {
			String law = kons.getFall().getRequiredString("Gesetz");
			TimeTool date = new TimeTool(kons.getDatum());
			
			// +++++ loop
			boolean skip = TarmedOptifierLists.skipCodesArray.contains(tcid);
			if (!skip) {
				/* special handling for all tarmed 5-minute-chunks with first-middle-last 5 minutes.
				 * Add the correct 5-minute-chunk code depending on how many chunks are already present
				 * and depending on the current age of the patient.
				 * Limit to maximum chunks defined in tarmed.
				 * if adding over 4 chunks for standard age patients: ask user once if this is ok
				*/
				int codeMapIx = -1;
				String joinedCodeMap = "";
				for (codeMapIx =
					0; codeMapIx < TarmedOptifierLists.fiveMinuteChunkCodeMaps.length; codeMapIx++) {
					String[] codeMap = TarmedOptifierLists.fiveMinuteChunkCodeMaps[codeMapIx];
					joinedCodeMap = "," + StringTool.join(codeMap, ",") + ",";
					if (joinedCodeMap.contains("," + tcid + ",")) {
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
						boolean isChildOrOlder = isNormalAgeForCode(kons, codeMap[3]);
						//						boolean isChildOrOlder = false; // *** default (NOT KVG) or old version/UVG-Version
						//						try {
						//							if (law.equalsIgnoreCase("KVG")) {
						//								IVerrechenbar childrensVerrechenbar =
						//									TarmedLeistung.getFromCode(codeMap[3], date, law);
						//								TarmedLeistung tc2 = (TarmedLeistung) childrensVerrechenbar;
						//								Hashtable<String, String> extChildren = tc2.loadExtension();
						//								String ageLimits =
						//									extChildren.get(TarmedLeistung.EXT_FLD_SERVICE_AGE);
						//								isChildOrOlder = true;
						//								if (ageLimits != null && !ageLimits.isEmpty()) {
						//									String errorMessage = checkAge(ageLimits, kons);
						//									if (errorMessage != null)
						//										isChildOrOlder = false;
						//								}
						//							}
						//						} catch (Exception ex) {
						//							// *** no children's code defined -> isChildOrOlder = false
						//						}
						
						// *** add 5-minute-chunks depending on numberOfFiveMinuteChunks, age, tarmed version, etc
						String newCode = tcid;
						switch (numberOfFiveMinuteChunks) {
						case 0:
							// *** first entry always: "first 5 minutes", [0]
							newCode = codeMap[0];
							lastKonsUsed = null;
							break;
						case 1:
							// *** if only two entries: add "last 5 minutes" [2]
							newCode = codeMap[2];
							lastKonsUsed = null;
							break;
						case 2:
						case 3:
							// *** if 15 or 20 minutes: add "middle 5 minutes" [1] or [4] (children)
							if (isChildOrOlder)
								newCode = codeMap[3];
							else
								newCode = codeMap[1];
							lastKonsUsed = null;
							break;
						case 4:
						case 5:
							// *** > 20, < 30 minutes: allowed for children and in special cases for others
							if (isKonsAfter2018 && codeMap.length > 3) {
								if (isChildOrOlder)
									newCode = codeMap[3]; // *** ad  "middle 5 minutes" [3]
								else {
									if (law.equalsIgnoreCase("KVG")) {
										// *** add "middle 5 minutes with more time needed" [4]
										boolean okToAdd = true;
										if (!kons.equals(lastKonsUsed)) {
											okToAdd = SWTHelper.askYesNo("Verrechnung",
												"Achtung: über 20 Minuten mit spezieller Notiz in der KG/Begründung für die KK");
										}
										if (okToAdd) {
											IVerrechenbar toBeAdded =
												TarmedLeistung.getFromCode(codeMap[4], date, law);
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
											lastKonsUsed = kons;
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
							} else
								// *** use middle 5 minutes and let the test go on regularly
								newCode = codeMap[1];
							break;
						case 6:
							if (codeMap.length > 3) {
								return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSION,
									"Sie können seit der Revision durch Alain Berset grunzipiell nicht mehr mehr als maximal 30 Minuten verrechnen.",
									null, false);
							} else {
								newCode = codeMap[1];
							}
							lastKonsUsed = null;
						default:
							newCode = codeMap[1];
							lastKonsUsed = null;
						}
						
						// **if the code has been changed, then recall proc with changed code and return
						if (!tcid.equals(newCode)) {
							IVerrechenbar toBeAdded =
								TarmedLeistung.getFromCode(newCode, date, law);
							
							code = toBeAdded;
							tc = (TarmedLeistung) toBeAdded;
							tcid = code.getCode();
							
							//++++++++return kons.addLeistung(toBeAdded);
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
		
		// +++++ again lst
		lst = kons.getLeistungen();
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
		
		// +++++ START minutes: moved above
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
