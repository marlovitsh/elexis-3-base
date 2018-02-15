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
	public static boolean doMinuteOptify = false;
	public static boolean doStripMinuteItemsFromTree = false;
	
	public static boolean optifierDisabled = false;
	
	protected Konsultation lastKonsUsed = null;
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
	
	// +++++ START children's additions
	
	/* Entweder oder:
	00.0730 "Punktion in Reservoirsystem (intravenös, intraarteriell, Liquor) durch den Facharzt";
	00.0740 "Punktion u/o Injektion in Reservoirsystem (Liquor) durch den Facharzt beim Kind/Jugendlichen bis 16 Jahre";
	
	00.0760 "Injektion, intrakutan/intramukös, durch den Facharzt (Bestandteil von 'Allgemeine Grundleistungen')";
	00.0770 FALSCH ALS SUBITEM!!! "+ Injektion, intrakutan/intramukös, durch den Facharzt, beim Kind bis 7 Jahre (Bestandteil von 'Allgemeine Grundleistungen')";
	
	00.0800 "Injektion, intravenös (Bestandteil von 'Allgemeine Grundleistungen')";
	00.0810 FALSCH ALS SUBITEM!!!"+ Injektion, intravenös, beim Kind bis 7 Jahre (Bestandteil von 'Allgemeine Grundleistungen')";
	
	00.0890 "Gefässzugang, Venaesectio durch den Facharzt beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen"
	00.0900 "Gefässzugang, Venaesectio durch den Facharzt beim Kind bis 7 Jahre" (7 Jahre (+30 Tage))
	00.0910 "Gefässzugang, Venaesectio durch den Facharzt bei Frühgeborenen und Neugeborenen" (<= 1 Monat (+ 7 Tage) )
	
	00.0980 "Einlage eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze"
	00.0990 FALSCH ALS SUBITEM!!! "+ Einlage eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze, beim Kind bis 7 Jahre"
	
	00.0995 "Entfernung eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze";
	00.0996 FALSCH ALS SUBITEM!!! "+ Entfernung eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze, beim Kind bis 7 Jahre";
	
	01.0210 "Härtende Verbände (Zirkulärverbände/Schienen), Kategorie I";
	01.0250 "+ Zuschlag bei härtenden Verbänden beim Kind bis 7 Jahre";
	
	Kapitel 03 +++++ not yet implemented
	Kapitel 05 +++++ not yet implemented
	
	08.0480 ALTER WIRD NOCH NICHT GETESTET "Orientierende Motilitätsprüfung und Stereopsisprüfung beim Kind bis 7 Jahre, beidseitig";""
	
	08.0650 "Tränenwegsondierung, einseitig";
	08.0660 "+ Zuschlag beim Kind bis 7 Jahre bei Tränenwegsondierung";
	
	08.2150 "Fremdkörperentfernung aus Kornea und Sklera, tiefe Lage, mit Entfernung des Rosthofes, erster Fremdkörper";
	08.2170 "+ Zuschlag für Fremdkörperentfernung(en) aus Kornea und Sklera beim Kind bis 7 Jahre";
	
	08.2760" Extractio lentis/Phakoemulsifikation, inkl. Implantation einer künstlichen Linse und Einsetzen eines Kapselspannringes";
	08.2830" + Zuschlag beim Kind bis 7 Jahre bei Extractio lentis/Phakoemulsifikation";
	
	09.0120 "Untersuchung mit Ohrmikroskop, pro Seite";
	09.0130 FALSCH ALS SUBITEM!!! "+ Untersuchung mit Ohrmikroskop beim Kind bis 7 Jahre";
	
	09.0310 "Reintonaudiogramm, Luftleitung, pro Seite";
	09.0320 "+ Zuschlag beim Kind bis 7 Jahre bei Reintonaudiogramm, Luftleitung, pro Seite";
	
	09.0340 "Reintonaudiogramm, Luftleitung und Knochenleitung, beidseitig";
	09.0350 "+ Zuschlag beim Kind bis 7 Jahre bei Reintonaudiogramm, Luftleitung und Knochenleitung, beidseitig";
	
	09.0560 "Registrierung otoakustischer Emissionen, beidseitig";
	09.0570 "+ Zuschlag beim Kind bis 7 Jahre bei Registrierung otoakustischer Emissionen, beidseitig";
	
	09.0930 "Erschwerte Gehörgangsreinigung mittels Mikroskop, pro Seite";
	09.0940 "+ Zuschlag beim Kind bis 7 Jahre bei erschwerter Gehörgangsreinigung mittels Mikroskop, pro Seite";
	
	09.1105 "Parazentese des Trommelfelles beim Erwachsenen älter als 16 Jahre, pro Seite, als alleinige Leistung";
	09.1106 "+ Transtympanische Mittelohrtoilette bei Parazentese des Trommelfells beim Erwachsenen älter als 16 Jahre, pro Seite";
	09.1107 "+ Einlage eines Röhrchens bei Parazentese des Trommelfells beim Erwachsenen älter als 16 Jahre, pro Seite";
	09.1110 "Parazentese des Trommelfelles beim Kind/Jugendlichen bis 16 Jahre, pro Seite, als alleinige Leistung";
	09.1120 "+ Transtympanische Mittelohrtoilette bei Parazentese des Trommelfells beim Kind/Jugendlichen bis 16 Jahre , pro Seite";
	09.1130 "+ Einlage eines Röhrchens bei Parazentese des Trommelfells beim Kind/Jugendlichen bis 16 Jahre , pro Seite";
	09.1140 "(+) Parazentese des Trommelfelles beim Kind/Jugendlichen bis 16 Jahre, pro Seite, als Zuschlagsleistung";
	09.1145 "(+) Parazentese des Trommelfelles beim Erwachsenen älter als 16 Jahre, pro Seite, als Zuschlagsleistung";
	
	10.0110 "Reposition einer Septumluxation beim Neugeborenen";
	
	10.0630 "Endonasale Fremdkörperextraktion aus dem mittleren/hinteren Drittel der Nasenhöhle";
	10.0640 "+ Zuschlag beim Kind bis 7 Jahre bei endonasaler Fremdkörperextraktion";
	
	15.0130 "Kleine Spirometrie mit Dokumentation der Flussvolumenkurve"
	15.0140 "+ Zuschlag beim Kind bis 7 Jahre bei Spirometrie mit Dokumentation der Flussvolumenkurve";
	
	15.0160 "Vollständige Spirometrie und Resistance (Plethysmografie)";
	15.0170 "+ Zuschlag beim Kind/Jugendlichen bis 16 Jahre bei vollständiger Spirometrie und Resistance (Plethysmografie)";
	
	15.0180 "Spirometrie und FRC-Messung/Plethysmografie beim Kind bis 3 Jahre";
	
	15.0410 "Bronchoskopie, starr, diagnostisch und therapeutisch";
	15.0430 "+ Zuschlag beim Kind bis 7 Jahre bei starrer/flexibler Bronchoskopie";
	
	17.0010 "Elektrokardiogramm ({EKG})";
	17.0040 "+ Zuschlag beim Kind bis 7 Jahre bei Elektrokardiogramm ({EKG})";
	
	17.0230 "Echokardiografie, transthorakal, Kontrolluntersuchung";
	17.0240 "Echokardiografie, transthorakal, beim Kind bis 3 Jahre";
	17.0250 "Echokardiografie, transthorakal, beim Kind/Jugendlichen ab 3 bis 16 Jahre";
	
	17.0260 "Echokardiografie, transoesophageal";
	17.0270 "+ Echokardiografie, transoesophageal beim Kind bis 7 Jahre";
	
	17.0710 "Kardangiografie, Grundleistung I";
	17.0720 "+ Zuschlag zur Grundleistung I beim Kind bis 7 Jahre";
	17.0730 "+ Zuschlag zur Grundleistung I beim Kind/Jugendlichen ab 7 bis 16 Jahre";
	
	19.0060 "Legen einer Magensonde durch den Facharzt";
	19.0070 "+ Zuschlag beim Kind bis 7 Jahre beim Legen einer Magensonde durch den Facharzt";
	
	19.1750 "Digitale Ausräumung des Rektums durch den Facharzt beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen";
	19.1760 "Digitale Ausräumung des Rektums durch den Facharzt beim Kind bis 7 Jahre";
	
	20.0220 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, einseitig";
	20.0250 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, beidseitig";
	20.0260 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, einseitig";
	20.0280 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, beidseitig";
	20.0290 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
	20.0300 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
	20.0310 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
	20.0320 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
	20.0330 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, tension-free, einseitig";
	20.0340 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, tension-free, beidseitig";
	20.0350 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, einseitig";
	20.0360 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, beidseitig";
	20.0370 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, einseitig";
	20.0410 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, beidseitig";
	20.0420 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
	20.0430 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
	20.0440 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
	20.0450 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
	20.0460 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, Inguinalisation tension-free, einseitig";
	20.0470 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, Inguinalisation tension-free, beidseitig";
	20.0480 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, einseitig";
	20.0490 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, beidseitig";
	
	20.1350 "Entfernung retroperitonealer Tumoren, beim Kind bis 7 Jahre, als alleinige Leistung exkl. Zugang";"20.2840"
	
	20.2840 "Entfernung retroperitonealer Tumoren, beim Kind/Jugendlichen und Erwachsenen älter als 7 Jahre, als alleinige Leistung exkl. Zugang";
	20.2850 "Operative Korrekturversorgung bei kongenitalen Darmanomalien/Malrotation im Frühkindesalter bis 2 Jahre";
	
	21.0010 "Blasenkatheterismus, diagnostisch und therapeutisch beim Knaben/Mann älter als 16 Jahre, durch den Facharzt";
	21.0020 "Blasenkatheterismus, diagnostisch und therapeutisch beim Mädchen/bei der Frau älter als 16 Jahre, durch den Facharzt";
	21.0030 "Blasenkatheterismus, diagnostisch und therapeutisch beim Kind/Jugendlichen bis 16 Jahre, durch den Facharzt";
	
	21.0110 "Urethroskopie, urethraler Zugang beim Knaben/Mann älter als 16 Jahre";
	21.0120 "+ Zuschlag für perinealen Zugang bei Urethroskopie";
	21.0130 "+ Biopsie bei Urethroskopie";
	21.0140 "+ Fulguration/Abtragung einer Läsion bei Urethroskopie, unabhängig der Anzahl";
	21.0150	"+ Steinentfernung(en)/Fremdkörperentfernung(en) bei Urethroskopie, unabhängig der Anzahl";
	21.0160 "Urethroskopie, urethraler Zugang beim Mädchen/bei der Frau älter als 16 Jahre";
	21.0170 "+ Zuschlag für perinealen Zugang bei Urethroskopie";
	21.0180 "+ Biopsie bei Urethroskopie";
	21.0190 "+ Fulguration/Abtragung einer Läsion bei Urethroskopie, unabhängig der Anzahl";
	21.0200 "+ Steinentfernung(en)/Fremdkörperentfernung(en) bei Urethroskopie, unabhängig der Anzahl";
	21.0210 "Urethroskopie, urethraler Zugang beim Kind/Jugendlichen bis 16 Jahre";
	21.0220 "+ Resektion posteriore Urethralklappen beim Kind/Jugendlichen bis 16 Jahre";
	
	///////////////
	21.0310 "Zystoskopie/Urethrozystoskopie beim Knaben/Mann älter als 16 Jahre";
	21.0320 "+ Diagnostische Endoskopie bei Zystoskopie/Urethrozystoskopie";
	21.0330 "+ Biopsie(n) bei Zystoskopie/Urethrozystoskopie";
	21.0340 "+ Fulguration/Abtragung einer Läsion bei Zystoskopie/Urethrozystoskopie, unabhängig der Anzahl";
	21.0350 "+ Steinentfernung(en)/Fremdkörperentfernung(en) bei Zystoskopie/Urethrozystoskopie, unabhängig der Anzahl";
	21.0360 "+ Lithotripsie, inkl. Trümmerentfernung, bei Zystoskopie/Urethrozystoskopie";
	21.0370 "+ Einlage Doppel-J-Katheter bei Zystoskopie/Urethrozystoskopie";
	21.0380 "+ Ureterenkatheterismus, bei Zystoskopie/Urethrozystoskopie, einseitig";
	21.0390 "+ Ureterenkatheterismus, bei Zystoskopie/Urethrozystoskopie, beidseitig";
	21.0400 "+ Steinmanipulation, Push-Back bei Zystoskopie/Urethrozystoskopie, retrograd, pro Seite";
	21.0410 "Zystoskopie/Urethrozystoskopie beim Mädchen/bei der Frau älter als 16 Jahre";
	21.0420 "+ Diagnostische Endoskopie bei Zystoskopie/Urethrozystoskopie";
	21.0430 "+ Biopsie(n) bei Zystoskopie/Urethrozystoskopie";
	21.0440 "+ Fulguration/Abtragung einer Läsion bei Zystoskopie/Urethrozystoskopie, unabhängig der Anzahl";
	21.0450 "+ Steinentfernung(en)/Fremdkörperentfernung(en) bei Zystoskopie/Urethrozystoskopie, unabhängig der Anzahl";
	21.0460 "Zystoskopie/Urethrozystoskopie beim Kind/Jugendlichen bis 16 Jahre";
	21.0470 "Zystoskopie durch Stoma";
	
	// +++++ weiterführen
	*/
	public static Object[][] childrensAdditions = {
		{
			"00.0010", "00.0040", 6
		}, {
			"00.0060", "00.0040", 6
		}, {
			"00.0060", "00.0040", 6
		},
	};
	// +++++ END children's additions
	
	// +++++ START minutes
	/**
	 * these string will be stripped/replaced from the original text shown in the tarmed tree when
	 * doing automatic 5-minute-chunk additions.
	 */
	// TODO languages 
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
	
	// *** the first  entry is first  5-minute-chunks for all patients - this will always be sent to this method
	// *** the second entry is middle 5-minute-chunks for 6-75 years
	// *** the third  entry is last   5-minute-chunks for all patients
	// *** the fourth entry is middle 5-minute-chunks for children/olders (max 4*)
	// *** the fifth  entry is middle 5-minute-chunks for 6-75 years with more time needed (max 4*)
	// *** test if added code is listed in minuteCodeMaps
	public static String[][] fiveMinuteChunkCodeMaps = {
		{
			"00.0010", "00.0020", "00.0030", "00.0025", "00.0026"
		}, {
			"00.0060", "00.0070", "00.0080", "00.0075", "00.0076"
		}, {
			"00.0110", "00.0120", "00.0130", "00.0125", "00.0126"
		}, {
			// ***"Telefonische, komplementärmedizinische Konsultation durch den Facharzt, 5 Min."
			"00.1880", "00.1890", "00.1900", "00.1895", "00.1896"
		}, {
			// *** Dermatologische Lasertherapie durch den Facharzt
			"04.0370", "04.0380", "04.0390"
		}, {
			// *** "Akupunktur, Konsultation durch den Facharzt, erste 5 Min.";"00.1710"
			"00.1710", "00.1720", "00.1730"
		}, {
			// *** "Neuraltherapie, Konsultation durch den Facharzt, erste 5 Min.";"00.1740"
			"00.1740", "00.1750", "00.1760"
		}, {
			// *** "Homöopathie, Konsultation durch den Facharzt, erste 5 Min.";"00.1770"
			"00.1770", "00.1780", "00.1790"
		}, {
			// *** "Traditionelle Chinesische Medizin ({TCM}), Konsultation durch den Facharzt, erste 5 Min.";"00.1810"
			"00.1810", "00.1820", "00.1830"
		}, {
			// *** "Anthroposophische Medizin, Konsultation durch den Facharzt, erste 5 Min.";"00.1840"
			"00.1840", "00.1850", "00.1860"
		}, {
			// *** "Phytotherapie durch Facharzt, Konsultation durch den Facharzt, erste 5 Min.";"00.1870"
			"00.1870", "00.1871", "00.1872"
		}
	};
	//	public static String[][] fiveMinuteChunkCodeMapsNoChildren = {
	//		{
	//			// "Akupunktur, Konsultation durch den Facharzt, erste 5 Min.";"00.1710"
	//			"00.1710", "00.1720", "00.1730"
	//		}, {
	//			// "Neuraltherapie, Konsultation durch den Facharzt, erste 5 Min.";"00.1740"
	//			"00.1740", "00.1750", "00.1760"
	//		}, {
	//			// "Homöopathie, Konsultation durch den Facharzt, erste 5 Min.";"00.1770"
	//			"00.1770", "00.1780", "00.1790"
	//		}, {
	//			// "Traditionelle Chinesische Medizin ({TCM}), Konsultation durch den Facharzt, erste 5 Min.";"00.1810"
	//			"00.1810", "00.1820", "00.1830"
	//		}, {
	//			// "Anthroposophische Medizin, Konsultation durch den Facharzt, erste 5 Min.";"00.1840"
	//			"00.1840", "00.1850", "00.1860"
	//		}, {
	//			// "Phytotherapie durch Facharzt, Konsultation durch den Facharzt, erste 5 Min.";"00.1870"
	//			"00.1870", "00.1871", "00.1872"
	//		}
	//	};
	
	public static String[][] ageGroupLists = {
		// *** col 1: 6-75
		// *** col 0: <6, >75
		// *** col 2: 6-75, erhöhter Aufwand
		{
			// "Kleine Untersuchung durch den Facharzt für Grundversorgung"
			"00.0415", "00.0416", "00.0417"
		}, {
			// "Kleine rheumatologische Untersuchung durch den Facharzt für Rheumatologie, Physikalische Medizin und Rehabilitation"
			"00.0435", "00.0436", "00.0437"
		}, {
			// "Spezifische Beratung durch den Facharzt für Grundversorgung"
			"00.0510", "00.0515", "00.0516"
		}, {
			// "Genetische u/o pränatale Beratung durch den Facharzt"
			"00.0530", "00.0535", "00.0536"
		}, {
			// "Instruktion von Selbstmessungen, Selbstbehandlungen durch den Facharzt"
			"00.0610", "00.0615", "00.0616"
		}, {
			//"Nachbetreuung/Betreuung/Überwachung in der Arztpraxis"
			"00.1370", "00.1375", "00.1376"
		}, {
			//"Telefonische Konsultation durch den Facharzt für Psychiatrie"
			"02.0060", "02.0065", "02.0066"
		}, {
			//"Telefonische Konsultation durch behandelnden Psychologen/Psychotherapeuten"
			"02.0150", "02.0155", "02.0156"
		}, {
			//"Untersuchung durch den Facharzt für Dermatologie"
			"04.0015", "04.0016", "04.0017"
		}, {
			//"Vorbesprechung diagnostischer/therapeutischer Eingriffe mit Patienten/Angehörigen durch den Facharzt"
			"00.0050", "00.0055", "00.0056"
		}, {
			//"Aktenstudium in Abwesenheit des Patienten"
			"00.0141", "00.0131", "00.0161"
		}, {
			//"Erkundigungen bei Dritten in Abwesenheit des Patienten"
			"00.0142", "00.0132", "00.0162"
		}, {
			//"Auskünfte an Angehörige oder andere Bezugspersonen des Patienten in Abwesenheit des Patienten"
			"00.0143", "00.0133", "00.0163"
		}, {
			//"Besprechungen mit Therapeuten und Betreuern des Patienten in Abwesenheit des Patienten"
			"00.0144", "00.0134", "00.0164"
		}, {
			//"Überweisungen an Konsiliarärzte in Abwesenheit des Patienten"
			"00.0145", "00.0135", "00.0165"
		}, {
			//"Ausstellen von Rezepten oder Verordnungen ausserhalb von Konsultation, Besuch und telefonischer Konsultation in Abwesenheit des Patienten"
			"00.0146", "00.0136", "00.0166"
		}, {
			//"Diagnostische Leistung am Institut für Pathologie/Histologie/Zytologie in Abwesenheit des Patienten"
			"00.0147", "00.0137", "00.0167"
		}, {
			//"Tumorboard in Abwesenheit des Patienten"
			"00.0148", "00.0138", "00.0168"
		}, {
			//"Telefonische Konsultation durch den Facharzt für Psychiatrie"
			"02.0060", "02.0065", "02.0066"
		}, {
			//"Telefonische Konsultation durch behandelnden Psychologen/Psychotherapeuten"
			"02.0150", "02.0155", "02.0156"
		}
	};
	
	public static String[][][] codeMapListArrays = {
		fiveMinuteChunkCodeMaps, /*fiveMinuteChunkCodeMapsNoChildren, */ageGroupLists
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
		// *** must add before getting lst (see below)
		if (bOptify) {
			TarmedLeistung[] matchingParents =
				getMatchingTarmedParents(tc, new TimeTool(kons.getDatum()));
			if (matchingParents.length > 0) {
				//			SWTHelper.alert("", "parent data should be added: \n" + matchingParents[0].getCode()
				//				+ " " + matchingParents[0].getText());
				//add(getKonsVerrechenbar(matchingParents[0].getCode(), kons), kons);
				Result<IVerrechenbar> addResult =
					kons.addLeistung(getKonsVerrechenbar(matchingParents[0].getCode(), kons));
				//return kons.addLeistung(tc);
			}
		}
		// +++++ END automatic addition of needed parent codes
		
		List<Verrechnet> lst = kons.getLeistungen();
		
		/*
		 * TODO Hier checken, ob dieser code mit der Dignität und
		 * Fachspezialisierung des aktuellen Mandanten usw. vereinbar ist
		 */
		
		Hashtable<String, String> ext = tc.loadExtension();
		// +++++ START minutes
		boolean isAfter2018 = new TimeTool(kons.getDatum()).get(Calendar.YEAR) >= 2018;
		String tcid = code.getCode();
		doMinuteOptify = false;
		if (doMinuteOptify && isAfter2018) {
			TimeTool date = new TimeTool(kons.getDatum());
			String law = kons.getFall().getRequiredString("Gesetz");
			
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
		if (doMinuteOptify && isAfter2018) {
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
						boolean isChildOrOlder = false; // *** default (NOT KVG) or old version/UVG-Version
						try {
							if (law.equalsIgnoreCase("KVG")) {
								IVerrechenbar childrensVerrechenbar =
									TarmedLeistung.getFromCode(codeMap[3], date, law);
								TarmedLeistung tc2 = (TarmedLeistung) childrensVerrechenbar;
								Hashtable<String, String> extChildren = tc2.loadExtension();
								String ageLimits =
									extChildren.get(TarmedLeistung.EXT_FLD_SERVICE_AGE);
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
						
						// *** 
						String newCode = tcid;
						switch (numberOfFiveMinuteChunks) {
						case 0:
							// *** first entry always: "erste 5 Min."
							newCode = codeMap[0];
							lastKonsUsed = null;
							break;
						case 1:
							// *** if only two entries: add "letzte 5 Min."
							newCode = codeMap[2];
							lastKonsUsed = null;
							break;
						case 2:
						case 3:
							// *** if 15 or 20 minutes: add "jede weiteren 5 Min."
							if (isChildOrOlder)
								newCode = codeMap[3];
							else
								newCode = codeMap[1];
							lastKonsUsed = null;
							break;
						case 4:
						case 5:
							if (codeMap.length > 3) {
								if (isChildOrOlder)
									newCode = codeMap[3];
								else {
									if (law.equalsIgnoreCase("KVG")) {
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
								newCode = codeMap[1];
							break;
						case 6:
							if (codeMap.length > 3) {
								return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, EXKLUSION,
									"mehr als 30 geht grunzipiell nicht.", //$NON-NLS-1$
									null, false);
							} else {
								newCode = codeMap[1];
							}
							lastKonsUsed = null;
						default:
							newCode = codeMap[1];
							lastKonsUsed = null;
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
				List<String> blubb = tl.getHierarchy(konsDate);
				TarmedLeistung tarm = TarmedLeistung.load(blubb.get(0));
				String txt = tarm.getText();
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
