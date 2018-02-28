package ch.elexis.views;

import java.util.ArrayList;

/**
 * Contains all the lists used by the optifier with links between items which are not clearly or
 * fully defined inside the official tarmed itself. Eg. age-groups, 5-minute-chunks,
 * children-additions, etc. This may be changing over time with changes of the tarmed. Do NOT place
 * anything else in this file.
 */
// +++++ %-Zuschläge nicht generell gelöst
// +++++ 32 65 70 bug
public class TarmedOptifierLists {
	/**
	 * Contains all the positions that define 5-minute-chunks in the form:
	 * <ul>
	 * <li>first 5 minutes</li>
	 * <li>middle 5 minutes</li>
	 * <li>last 5 minutes</li>
	 * </ul>
	 * And - since the second illegal modification by Alain Berset - also handles the distinction
	 * between age groups:
	 * <ul>
	 * <li>under 6 years or over 75 years</li>
	 * <li>from 6 to 75 years</li>
	 * <li>from 6 to 75 years with more time needed</li>
	 * </ul>
	 * Items order:
	 * <ul>
	 * <li>first 5-minute-chunks</li>
	 * <li>middle 5-minute-chunks</li>
	 * <li>last 5-minute-chunks</li>
	 * <li>middle 5-minute-chunks for childer/olders (usually max 4*) (if Berset's tarmed)</li>
	 * <li>middle 5-minute-chunks for 6-75 years with more time needed (usually max 4*) (if Berset's
	 * tarmed)</li>
	 * </ul>
	 */
	public final static String[][] fiveMinuteChunkCodeMaps = {
		{
			// *** "Konsultation, 5 Min."
			"00.0010", "00.0020", "00.0030", "00.0025", "00.0026"
		}, {
			// *** "Besuch, 5 Min.
			"00.0060", "00.0070", "00.0080", "00.0075", "00.0076"
		}, {
			// *** "Telefonische Konsultation durch den Facharzt, 5 Min."
			"00.0110", "00.0120", "00.0130", "00.0125", "00.0126"
		}, {
			// *** "Akupunktur, Konsultation durch den Facharzt, erste 5 Min."
			"00.1710", "00.1720", "00.1730"
		}, {
			// *** "Neuraltherapie, Konsultation durch den Facharzt, erste 5 Min."
			"00.1740", "00.1750", "00.1760"
		}, {
			// *** "Homöopathie, Konsultation durch den Facharzt, erste 5 Min."
			"00.1770", "00.1780", "00.1790"
		}, {
			// *** "Traditionelle Chinesische Medizin ({TCM}), Konsultation durch den Facharzt, erste 5 Min."
			"00.1810", "00.1820", "00.1830"
		}, {
			// *** "Anthroposophische Medizin, Konsultation durch den Facharzt, erste 5 Min."
			"00.1840", "00.1850", "00.1860"
		}, {
			// *** "Phytotherapie durch Facharzt, Konsultation durch den Facharzt, erste 5 Min."
			"00.1870", "00.1871", "00.1872"
		}, {
			// ***"Telefonische, komplementärmedizinische Konsultation durch den Facharzt, 5 Min."
			"00.1880", "00.1890", "00.1900", "00.1895", "00.1896"
		}, {
			// *** Dermatologische Lasertherapie durch den Facharzt
			"04.0370", "04.0380", "04.0390"
		}
	};
	
	// ***********************************************
	/**
	 * Defines automatic children's additions dependent on age of the patient at the date of the
	 * konsultation.<br>
	 * Definition:
	 * <ul>
	 * <li>first item: Base Tarmed</li>
	 * <li>following items: children's addition</li>
	 * </ul>
	 */
	public final static String[][] childrensAdditions = {
		{
			// *** Konsultation, erste 5 Min.
			"00.0010", "00.0040"
		}, {
			// *** Härtende Verbände (Zirkulärverbände/Schienen), Kategorie I
			// *** + Zuschlag bei härtenden Verbänden beim Kind bis 7 Jahre"
			"01.0210", "01.0250"
		}, {
			// *** 08.0650 "Tränenwegsondierung, einseitig";
			// *** 08.0660 "+ Zuschlag beim Kind bis 7 Jahre bei Tränenwegsondierung";
			"08.0650", "08.0660"
		}, {
			// *** Dies ist ein Zuschlag, auch wenn sprachlich nicht explizit als solcher deklariert
			// *** 00.0760 "Injektion, intrakutan/intramukös, durch den Facharzt (Bestandteil von 'Allgemeine Grundleistungen')";
			// *** 00.0770 "+ Injektion, intrakutan/intramukös, durch den Facharzt, beim Kind bis 7 Jahre (Bestandteil von 'Allgemeine Grundleistungen')"; FALSCH ALS SUBITEM!!! 
			"00.0760", "00.0770"
		}, {
			// *** Dies ist ein Zuschlag, auch wenn sprachlich nicht explizit als solcher deklariert
			// *** 00.0800 "Injektion, intravenös (Bestandteil von 'Allgemeine Grundleistungen')";
			// *** 00.0810 "+ Injektion, intravenös, beim Kind bis 7 Jahre (Bestandteil von 'Allgemeine Grundleistungen')"; FALSCH ALS SUBITEM!!!
			"00.0800", "00.0810"
		}, {
			// *** Dies ist ein Zuschlag, auch wenn sprachlich nicht explizit als solcher deklariert
			// *** 00.0980 "Einlage eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze"
			// *** 00.0990 "+ Einlage eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze, beim Kind bis 7 Jahre" FALSCH ALS SUBITEM!!!
			"00.0980", "00.0990"
		}, {
			// *** Dies ist ein Zuschlag, auch wenn sprachlich nicht explizit deklariert
			// *** 00.0995 "Entfernung eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze";
			// *** 00.0996 "+ Entfernung eines Port-A-Cath/arteriovenösen Reservoirsystems, venös/arteriell, jede Lokalisation der Katheterspitze, beim Kind bis 7 Jahre"; FALSCH ALS SUBITEM!!!
			"00.0995", "00.0996"
		}, {
			// *** 03.0560 "Registrierung otoakustischer Emissionen, beidseitig, beim Kind/Jugendlichen bis 18 Jahre durch den Facharzt für Kinder- und Jugendmedizin";
			// *** 03.0570 "+ Zuschlag beim Kind bis 7 Jahre bei Registrierung otoakustischer Emissionen, beidseitig";
			"03.0560", "03.0570"
		}, {
			// *** 08.2150 "Fremdkörperentfernung aus Kornea und Sklera, tiefe Lage, mit Entfernung des Rosthofes, erster Fremdkörper";
			// *** 08.2170 "+ Zuschlag für Fremdkörperentfernung(en) aus Kornea und Sklera beim Kind bis 7 Jahre";
			"08.2150", "08.2170"
		}, {
			// *** 08.2760" Extractio lentis/Phakoemulsifikation, inkl. Implantation einer künstlichen Linse und Einsetzen eines Kapselspannringes";
			// *** 08.2830" + Zuschlag beim Kind bis 7 Jahre bei Extractio lentis/Phakoemulsifikation";
			"08.2760", "08.2830"
		}, {
			// *** Dies ist ein Zuschlag, auch wenn sprachlich nicht explizit deklariert
			// *** 09.0120 "Untersuchung mit Ohrmikroskop, pro Seite";
			// *** 09.0130 "+ Untersuchung mit Ohrmikroskop beim Kind bis 7 Jahre"; FALSCH ALS SUBITEM!!!
			"09.0120", "09.0130"
		}, {
			// *** 09.0310 "Reintonaudiogramm, Luftleitung, pro Seite";
			// *** 09.0320 "+ Zuschlag beim Kind bis 7 Jahre bei Reintonaudiogramm, Luftleitung, pro Seite";
			"09.0310", "09.0320"
		}, {
			// *** 09.0340 "Reintonaudiogramm, Luftleitung und Knochenleitung, beidseitig";
			// *** 09.0350 "+ Zuschlag beim Kind bis 7 Jahre bei Reintonaudiogramm, Luftleitung und Knochenleitung, beidseitig";
			"09.0340", "09.0350"
		}, {
			// *** 09.0560 "Registrierung otoakustischer Emissionen, beidseitig";
			// *** 09.0570 "+ Zuschlag beim Kind bis 7 Jahre bei Registrierung otoakustischer Emissionen, beidseitig";
			"09.0560", "09.0570"
		}, {
			// *** 09.0930 "Erschwerte Gehörgangsreinigung mittels Mikroskop, pro Seite";
			// *** 09.0940 "+ Zuschlag beim Kind bis 7 Jahre bei erschwerter Gehörgangsreinigung mittels Mikroskop, pro Seite";
			"09.0930", "09.0940"
		}, {
			// *** 10.0630 "Endonasale Fremdkörperextraktion aus dem mittleren/hinteren Drittel der Nasenhöhle";
			// *** 10.0640 "+ Zuschlag beim Kind bis 7 Jahre bei endonasaler Fremdkörperextraktion";
			"10.0630", "10.0640"
		}, {
			// *** 15.0130 "Kleine Spirometrie mit Dokumentation der Flussvolumenkurve"
			// *** 15.0140 "+ Zuschlag beim Kind bis 7 Jahre bei Spirometrie mit Dokumentation der Flussvolumenkurve";
			"15.0130", "15.0140"
		}, {
			// *** 15.0160 "Vollständige Spirometrie und Resistance (Plethysmografie)";
			// *** 15.0170 "+ Zuschlag beim Kind/Jugendlichen bis 16 Jahre bei vollständiger Spirometrie und Resistance (Plethysmografie)";
			"15.0160", "15.0170"
		}, {
			// *** 15.0410 "Bronchoskopie, starr, diagnostisch und therapeutisch";
			// *** 15.0430 "+ Zuschlag beim Kind bis 7 Jahre bei starrer/flexibler Bronchoskopie";
			"15.0410", "15.0430"
		}, {
			// *** 17.0010 "Elektrokardiogramm ({EKG})";
			// *** 17.0040 "+ Zuschlag beim Kind bis 7 Jahre bei Elektrokardiogramm ({EKG})";
			"17.0010", "17.0040"
		}, {
			// *** Dies ist ein Zuschlag, auch wenn sprachlich nicht explizit deklariert
			// *** 17.0260 "Echokardiografie, transoesophageal";
			// *** 17.0270 "+ Echokardiografie, transoesophageal beim Kind bis 7 Jahre"; FALSCH ALS SUBITEM!!!
			"17.0260", "17.0270"
		}, {
			// *** 19.0060 "Legen einer Magensonde durch den Facharzt";
			// *** 19.0070 "+ Zuschlag beim Kind bis 7 Jahre beim Legen einer Magensonde durch den Facharzt";
			"19.0060", "19.0070"
		}, {
			// *** 19.1750 "Digitale Ausräumung des Rektums durch den Facharzt beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen";
			// *** 19.1760 "Digitale Ausräumung des Rektums durch den Facharzt beim Kind bis 7 Jahre";
			"19.1750", "19.1760"
		}, {
			// *** 17.0710 "Kardangiografie, Grundleistung I";
			// *** 17.0720 "+ Zuschlag zur Grundleistung I beim Kind bis 7 Jahre";
			// *** 17.0730 "+ Zuschlag zur Grundleistung I beim Kind/Jugendlichen ab 7 bis 16 Jahre";
			"17.0710", "17.0720", "17.0730"
		}, {
			// *** 28.0010 "Perioperative Betreuung durch den Facharzt für Anästhesie, Monitored Anesthesia Care (MAC)";
			// *** 28.0020 "+ %-Zuschlag für Patienten älter als 70 Jahre";
			"28.0010", "28.0020"
		}, {
			// 31.0270 "+ Elektrokardiogramm (EKG)"
			// 31.0280 "+ Zuschlag beim Kind bis 7 Jahre bei Elektrokardiogramm (EKG)/Arbeitsversuch"
			"31.0270", "31.0280"
		}, {
			// 31.0310 + Belastungs-EKG, Arbeitsversuch
			// 31.0280 "+ Zuschlag beim Kind bis 7 Jahre bei Elektrokardiogramm (EKG)/Arbeitsversuch"
			"31.0310", "31.0280"
		}, {
			// 32.0665 + Anästhesie bei Stereotaktischer Radiochirurgie. Einmalbestrahlung mit Gamma-Knife-Technik
			// 32.0670 + Zuschlag für Patienten älter als 70 Jahre
			"32.0665", "32.0670"
		}, {
			// *** 39.6000 "Arteriografie, Grundleistung I"
			// *** 39.6005 "+ Zuschlag zur Grundleistung I beim Kind bis 7 Jahre"
			// *** 39.6006 "+ Zuschlag zur Grundleistung I beim Kind/Jugendlichen ab 7 bis 16 Jahre"
			"39.6000", "39.6005", "39.6006"
		}, {
			// *** 39.6040 "Arteriografie, Grundleistung II"
			// *** 39.6055 "+ Zuschlag zur Grundleistung II beim Kind bis 7 Jahre"
			"39.6040", "39.6055"
		}, {
			// *** 39.6300 "Phlebografie/venöse Intervention, Grundleistung I mittels Direktpunktion"
			// *** 39.6315 "+ Zuschlag zur Grundleistung beim Kind bis 7 Jahre"
			// *** 39.6316 2+ Zuschlag zur Grundleistung beim Kind/Jugendlichen ab 7 bis 16 Jahre2
			"39.6300", "39.6315", "39.6316"
		}, {
			// *** 39.6640 "Angiografie Lunge, Grundleistung I"
			// *** 39.6650 "+ Zuschlag zur Grundleistung beim Kind/Jugendlichen bis 16 Jahre"
			"39.6640", "39.6650"
		},
	};
	
	/**
	 * Tarmed positions which are actually the same - just the age range is different.<br>
	 * You MUST sort the positions descending by age!<br>
	 * You can't test them safely by the data supplied by admin.ch, so we need a hand-made list...
	 * Would be nice to have better data from / better people at admin.ch...
	 */
	public final static String[][] ageConnections = {
		//{
		// +++++ this is NOT the same thing...
		// *** 00.0730 "Punktion in Reservoirsystem (intravenös, intraarteriell, Liquor) durch den Facharzt";
		// *** 00.0740 "Punktion u/o Injektion in Reservoirsystem (Liquor) durch den Facharzt beim Kind/Jugendlichen bis 16 Jahre";
		// "00.0730", "00.0740"
		//},
		{
			// *** 00.0890 "Gefässzugang, Venaesectio durch den Facharzt beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen"
			// *** 00.0900 "Gefässzugang, Venaesectio durch den Facharzt beim Kind bis 7 Jahre" (7 Jahre (+30 Tage))
			// *** 00.0910 "Gefässzugang, Venaesectio durch den Facharzt bei Frühgeborenen und Neugeborenen" (<= 1 Monat (+ 7 Tage) )
			"00.0890", "00.0900", "00.0910"
		}, {
			// *** 03.0120 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, ab 14 bis 16 Jahre";
			// *** 03.0110 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 10. Jahr";
			// *** 03.0100 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, ab 6 bis 7 Jahre";
			// *** 03.0090 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 5. Jahr";
			// *** 03.0080 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 24. Monat";
			// *** 03.0070 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, vom 15. bis 18. Monat";
			// *** 03.0060 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, vom 9. bis 12. Monat";
			// *** 03.0050 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 6. Monat";
			// *** 03.0040	"Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 4. Monat";
			// *** 03.0030	"Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 2. Monat";
			// *** 03.0020 "Vorsorgeuntersuchungen gemäss Empfehlungen {SGP}'93, im 1. Monat";
			"03.0120", "03.0110", "03.0100", "03.0090", "03.0080", "03.0070", "03.0060", "03.0050",
			"03.0040", "03.0030", "03.0020"
		}, {
			// *** 03.0330 "Entwicklungspädiatrische Untersuchungen beim Kind/Jugendlichen ab 7 bis 18 Jahre, partiell, pro 5 Min.";
			// *** 03.0310 "Entwicklungspädiatrische Untersuchungen beim Kind bis 7 Jahre, partiell, pro 5 Min.";
			"03.0330", "03.0310"
		}, {
			// *** 03.0340 "Entwicklungspädiatrische Untersuchungen beim Kind/Jugendlichen ab 7 bis 18 Jahre, umfassend, pro 5 Min.";
			// *** 03.0320 "Entwicklungspädiatrische Untersuchungen beim Kind bis 7 Jahre, umfassend, pro 5 Min.";
			"03.0340", "03.0320"
		}, {
			// *** 03.0430 "Neuromotorische Untersuchungen beim Kind/Jugendlichen ab 7 bis 18 Jahre, partiell, pro 5 Min.";
			// *** 03.0410 "Neuromotorische Untersuchungen beim Kind bis 7 Jahre, partiell, pro 5 Min.";
			"03.0430", "03.0410"
		}, {
			// *** 03.0440 "Neuromotorische Untersuchungen beim Kind/Jugendlichen ab 7 bis 18 Jahre, umfassend, pro 5 Min.";
			// *** 03.0420 "Neuromotorische Untersuchungen beim Kind bis 7 Jahre, umfassend, pro 5 Min.";
			"03.0440", "03.0420"
		}, {
			// *** 09.1105 "Parazentese des Trommelfelles beim Erwachsenen älter als 16 Jahre, pro Seite, als alleinige Leistung";
			// *** 09.1110 "Parazentese des Trommelfelles beim Kind/Jugendlichen bis 16 Jahre, pro Seite, als alleinige Leistung";
			"09.1105", "09.1110"
		}, {
			// *** 09.1106 "+ Transtympanische Mittelohrtoilette bei Parazentese des Trommelfells beim Erwachsenen älter als 16 Jahre, pro Seite";""
			// *** 09.1120 "+ Transtympanische Mittelohrtoilette bei Parazentese des Trommelfells beim Kind/Jugendlichen bis 16 Jahre , pro Seite";
			"09.1106", "09.1120"
		}, {
			// *** 09.1107 "+ Einlage eines Röhrchens bei Parazentese des Trommelfells beim Erwachsenen älter als 16 Jahre, pro Seite";
			// *** 09.1130 "+ Einlage eines Röhrchens bei Parazentese des Trommelfells beim Kind/Jugendlichen bis 16 Jahre , pro Seite";
			"09.1107", "09.1130"
		}, {
			// *** 17.0230 "Echokardiografie, transthorakal, Kontrolluntersuchung";
			// *** 17.0250 "Echokardiografie, transthorakal, beim Kind/Jugendlichen ab 3 bis 16 Jahre";
			// *** 17.0240 "Echokardiografie, transthorakal, beim Kind bis 3 Jahre";
			"17.0230", "17.0250", "17.0240"
		}, {
			// *** 20.0330 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, tension-free, einseitig";
			// *** 20.0290 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
			// *** 20.0310 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
			// *** 20.0260 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, einseitig";
			// *** 20.0220 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, einseitig";
			"20.0330", "20.0290", "20.0310", "20.0260", "20.0220"
		}, {
			// *** 20.0350 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, einseitig";
			// *** 20.0290 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
			// *** 20.0310 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
			// *** 20.0260 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, einseitig";
			// *** 20.0220 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, einseitig";
			"20.0350", "20.0290", "20.0310", "20.0260", "20.0220"
		}, {
			// *** 20.0340 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, tension-free, beidseitig";
			// *** 20.0280 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, beidseitig";
			// *** 20.0300 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0320 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0250 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, beidseitig";
			"20.0340", "20.0280", "20.0300", "20.0320", "20.0250"
		}, {
			// *** 20.0360 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, beidseitig";
			// *** 20.0300 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0320 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0280 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, beidseitig";
			// *** 20.0250 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, beidseitig";
			"20.0360", "20.0300", "20.0320", "20.0280", "20.0250"
		}, {
			// *** 20.0460 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, Inguinalisation tension-free, einseitig";
			// *** 20.0420 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
			// *** 20.0440 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
			// *** 20.0370 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, einseitig";
			"20.0460", "20.0420", "20.0440", "20.0370"
		}, {
			// *** 20.0480 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, einseitig";
			// *** 20.0420 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
			// *** 20.0440 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
			// *** 20.0370 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, einseitig";
			"20.0480", "20.0420", "20.0440", "20.0370"
		}, {
			// *** 20.0470 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, Inguinalisation tension-free, beidseitig";
			// *** 20.0430 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0450 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0410 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, beidseitig";
			"20.0470", "20.0430", "20.0450", "20.0410"
		}, {
			// *** 20.0490 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, beidseitig";
			// *** 20.0430 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0450 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0410 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, beidseitig";
			"20.0490", "20.0430", "20.0450", "20.0410"
		}, {
			// *** 21.0010 "Blasenkatheterismus, diagnostisch und therapeutisch beim Knaben/Mann älter als 16 Jahre, durch den Facharzt";
			// *** 21.0020 "Blasenkatheterismus, diagnostisch und therapeutisch beim Mädchen/bei der Frau älter als 16 Jahre, durch den Facharzt";
			// *** 21.0030 "Blasenkatheterismus, diagnostisch und therapeutisch beim Kind/Jugendlichen bis 16 Jahre, durch den Facharzt";
			"20.0490", "20.0410", "20.0430", "20.0450"
		}, {
			// *** 20.2850 "Entfernung retroperitonealer Tumoren, beim Kind/Jugendlichen und Erwachsenen älter als 7 Jahre, als alleinige Leistung exkl. Zugang";
			// *** 20.2840 "Entfernung retroperitonealer Tumoren, beim Kind bis 7 Jahre, als alleinige Leistung exkl. Zugang";
			"20.2850", "20.2840"
		}, {
			// *** 21.3130 "Operative Versorgung bei Kryptorchismus beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen, einseitig";
			// *** 21.3135 "Operative Versorgung bei Kryptorchismus, beim Kind bis 7 Jahre, einseitig";
			"21.3130", "21.3135"
		}, {
			// *** 21.3140 "Operative Versorgung bei Kryptorchismus beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen, beidseitig";
			// *** 21.3145 "Operative Versorgung bei Kryptorchismus, beim Kind bis 7 Jahre, beidseitig"
			"21.3140", "21.3145"
		},
			
			// +++++ noch vervollständigen
			// ", beim Kind bis 7 Jahre"
			// " beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen"
			// " beim Kind bis 7 Jahre" (7 Jahre (+30 Tage))"
			// " bei Frühgeborenen und Neugeborenen" (<= 1 Monat (+ 7 Tage) )"
	};
	
	/**
	 * <ul>
	 * <li>male</li>
	 * <li>female</li>
	 * <li>others</li>
	 * </ul>
	 */
	public final static String[][] sexConnections = {
		{
			// *** 20.0310 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
			// *** 20.0290 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
			//// *** 20.0220 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, einseitig";
			//// *** 20.0260 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, einseitig";
			// *** 20.0330 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, tension-free, einseitig";
			// *** 20.0350 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, einseitig";
			"20.0310", "20.0290", /*"20.0220", "20.0260", */"20.0330", "20.0350"
		}, {
			// *** 20.0320 "Operative Versorgung einer Inguinalhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0300 "Operative Versorgung einer Inguinalhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
			//// *** 20.0250 "Operative Versorgung einer Inguinalhernie beim Neugeborenen, beidseitig";
			//// *** 20.0280 "Operative Versorgung einer Inguinalhernie beim Kind bis 7 Jahre, beidseitig";
			// *** 20.0340 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, tension-free, beidseitig";
			// *** 20.0360 "Operative Versorgung einer Inguinalhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, beidseitig";
			"20.0320", "20.0300", /*"20.0250", "20.0280",*/ "20.0340", "20.0360"
		}, {
			// *** 20.0440 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, einseitig";
			// *** 20.0420 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, einseitig";
			// *** 20.0370 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, einseitig";
			// *** 20.0460 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, Inguinalisation tension-free, einseitig";
			// *** 20.0480 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, einseitig";
			"20.0440", "20.0420", "20.0370", "20.0460", "20.0480"
		}, {
			// *** 20.0450 "Operative Versorgung einer Femoralhernie beim Knaben ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0430 "Operative Versorgung einer Femoralhernie beim Mädchen ab 7 bis 16 Jahre, beidseitig";
			// *** 20.0410 "Operative Versorgung einer Femoralhernie beim Kind bis 7 Jahre, beidseitig";
			// *** 20.0470 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, Inguinalisation tension-free, beidseitig";
			// *** 20.0490 "Operative Versorgung einer Femoralhernie beim Erwachsenen älter als 16 Jahre, jede Methode, exkl. tension-free, beidseitig";
			"20.0450", "20.0430", "20.0410", "20.0470", "20.0490"
		}, {
			// *** 21.0010 "Blasenkatheterismus, diagnostisch und therapeutisch beim Knaben/Mann älter als 16 Jahre, durch den Facharzt";
			// *** 21.0020 "Blasenkatheterismus, diagnostisch und therapeutisch beim Mädchen/bei der Frau älter als 16 Jahre, durch den Facharzt";
			// *** 21.0030 "Blasenkatheterismus, diagnostisch und therapeutisch beim Kind/Jugendlichen bis 16 Jahre, durch den Facharzt";
			"21.0010", "21.0020", "21.0030"
		}
	};
	
	/* Entweder oder:
	
		Kapitel 05 +++++ not yet implemented
		
		????:
		09.1140 "(+) Parazentese des Trommelfelles beim Kind/Jugendlichen bis 16 Jahre, pro Seite, als Zuschlagsleistung";
		09.1145 "(+) Parazentese des Trommelfelles beim Erwachsenen älter als 16 Jahre, pro Seite, als Zuschlagsleistung";
		
		AUCH NOCH GESCHLECHT:
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
		
		AUCH NOCH GESCHLECHT:
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
		
		?????:
		21.0810 "Uroflowmetrie";
		21.0820 "Uroflowmetrie mit Beckenboden-{EMG} beim Kind/Jugendlichen bis 16 Jahre";
	
		22.0030 "Spezielle gynäkologische Beratung durch den Facharzt, pro 5 Min.";
		22.0035 "Spezielle gynäkologische Beratung durch den Facharzt bei Personen über 6 Jahren und unter 75 Jahren mit einem erhöhten Behandlungsbedarf, pro 5 Min."
		
		22.0040 "Sterilitätsberatung für die Frau durch den Facharzt für Gynäkologie, pro 5 Min.";
		22.0045 "Sterilitätsberatung für die Frau durch den Facharzt für Gynäkologie bei Personen über 6 Jahren und unter 75 Jahren mit einem erhöhten Behandlungsbedarf, pro 5 Min."
		
		Tests für vorpubertär/Kind/Menarche???
		22.0210 "Untersuchung des weiblichen Genitale beim vorpubertären Kind, ohne Spekulum";
		22.0230 "Vaginoskopie beim Kind (vor der Menarche)";
		
		?????:
		32.0110 "(+) Narkose/{IPS}-Zuschlag beim Kind/Jugendlichen älter als 7 Jahre und Erwachsenen";
		32.0120 "(+) Zuschlag I für Kinder bis 7 Jahre";
	
		?????:
		39.0050 (+) Zuschlag I ohne Kontrastmittel und/oder Hohlraumfüllungen, für Kinder bis 7 Jahre
		39.0055 (+) Zuschlag II mit Kontrastmittel und/oder Hohlraumfüllungen, für Kinder bis 7 Jahre
		
		*/
	/**
	 * These strings will be stripped/replaced from the original text shown in the tarmed tree when
	 * doing automatic 5-minute-chunk additions.
	 */
	// TODO languages 
	public final static String[][] itemReplacements5MinuteCodes = {
		{
			" (Grundkonsultation)", ""
		}, {
			" (consultation de base)", ""
		}, {
			" (consultazione di base)", ""
		},
		
		{
			" (Konsultationszuschlag)", ""
		}, {
			" (supplément de consultation)", ""
		}, {
			" (supplément de consultation)", ""
		},
		
		{
			" (Grundbesuch)", ""
		}, {
			" (visite de base)", ""
		}, {
			" (Visita a domicilio di base)", ""
		},
		
		{
			" (Besuchszuschlag)", ""
		}, {
			" (Besuchszuschlag_none)", ""
		}, {
			" (Besuchszuschlag_none)", ""
		},
		
		{
			" erste 5 Min.", " 5 Min."
		}, {
			" première période de 5 min", " 5 min"
		}, {
			" i primi 5 min.", " 5 min."
		},
		
		{
			" letzte 5 Min.", " 5 Min."
		}, {
			" dernière période de 5 min", " 5 min"
		}, {
			" ultimi 5 min.", " 5 min."
		},
		
		{
			" jede weiteren 5 Min.", " 5 Min."
		}, {
			" par période de 5 min en plus", " 5 min"
		}, {
			" par période de 5 min en plus", " 5 min"
		},
		
		{
			" bei Personen über 6 Jahren und unter 75 Jahren", ""
		}, {
			" pour les personnes au-dessus de 6 ans et de moins de 75 ans", ""
		}, {
			" per le persone di più di 6 anni e sotto 75 anni", ""
		},
		
		{
			" bei Kindern unter 6 Jahren und Personen über 75 Jahren", ""
		}, {
			" pour les enfants de moins de 6 ans et pour les personnes au-dessus de 75 ans", ""
		}, {
			" per i bambini sotto i sei anni e per le persone di più di 75 anni", ""
		},
		
		{
			" bei Personen über 6 Jahren und unter 75 Jahren mit einem erhöhten Behandlungsbedarf",
			""
		}, {
			" pour les personnes au-dessus de 6 ans et de moins de 75 ans nécessitant plus de soins",
			""
		}, {
			" per persone di più di 6 anni e sotto i 75 anni più bisognose di cure", ""
		},
	};
	
	/**
	 * these tarmeds must ALWAYS be combined. So - if you add one of them, you need to add the other
	 * one, too. The first one is the parent, the second one the child.
	 */
	public final static String[][] connectedTarmeds = {
		{
			"04.0550", // *** "Exzision von Hautprozessen (Tumor, entzündliche Prozesse, Schmutztätowierung, Tätowierung, Narbenfläche): Gesicht, Hals (ohne Nacken), Hand, mehr als 2 cm² Exzisat, erste 2 cm²"
			"04.0560" // *** "+ Exzision von Hautprozessen (Tumor, entzündliche Prozesse, Schmutztätowierung, Tätowierung, Narbenfläche): Gesicht, Hals (ohne Nacken), Hand, mehr als 2 cm² Exzisat, jede weitere 2 cm²"
		}, {
			"04.0590", // *** "Exzision von Hautprozessen (Tumor, entzündliche Prozesse, Schmutztätowierung, Tätowierung, Narbenfläche): übrige Regionen, mehr als 5 {cm2}, erste 5 {cm2}"
			"04.0600" // *** "+ Exzision von Hautprozessen (Tumor, entzündliche Prozesse, Schmutztätowierung, Tätowierung, Narbenfläche): übrige Regionen, mehr als 5 {cm2}, jede weiteren 5 {cm2}"
		}, {
			"04.0630", // ***"Exzision subkutaner Prozess: Gesicht, Hals (ohne Nacken), Hand, mehr als 2 cm Exzisat, erste 2 cm (max. Durchmesser)"
			"04.0640" // *** "+ Exzision subkutaner Prozess: Gesicht, Hals (ohne Nacken), Hand, mehr als 2 cm Exzisat, jede weitere 2 cm (max. Durchmesser)"
		}, {
			"04.0880", // *** "Exzision subkutaner Prozess, übrige Regionen, mehr als 5 cm Exzisat, erste 5 cm (max. Durchmesser)"
			"04.0890" // *** "+ Exzision subkutaner Prozess, übrige Regionen, mehr als 5 cm Exzisat, jede weiteren 3 cm Exzisat (max. Durchmesser)"
		},
	};
	
	/**
	 * Age group list. Defines groups of actually equal treatments which differ only in the age
	 * groups.<br>
	 * The age groups are defined as follows:
	 * <ul>
	 * <li>first column: "normal age", usually 6 - 75</li>
	 * <li>second column: children or older people, usually <6 or >75</li>
	 * <li>third column: "normal age, increased time requirement", usually 6 - 75</li>
	 * </ul>
	 */
	public final static String[][] ageGroupLists = {
		{
			// *** "Kleine Untersuchung durch den Facharzt für Grundversorgung"
			"00.0415", "00.0416", "00.0417"
		}, {
			// *** "Kleine rheumatologische Untersuchung durch den Facharzt für Rheumatologie, Physikalische Medizin und Rehabilitation"
			"00.0435", "00.0436", "00.0437"
		}, {
			// *** "Spezifische Beratung durch den Facharzt für Grundversorgung"
			"00.0510", "00.0515", "00.0516"
		}, {
			// *** "Genetische u/o pränatale Beratung durch den Facharzt"
			"00.0530", "00.0535", "00.0536"
		}, {
			// *** "Instruktion von Selbstmessungen, Selbstbehandlungen durch den Facharzt"
			"00.0610", "00.0615", "00.0616"
		}, {
			// *** "Nachbetreuung/Betreuung/Überwachung in der Arztpraxis"
			"00.1370", "00.1375", "00.1376"
		}, {
			// *** "Telefonische Konsultation durch den Facharzt für Psychiatrie"
			"02.0060", "02.0065", "02.0066"
		}, {
			// *** "Telefonische Konsultation durch behandelnden Psychologen/Psychotherapeuten"
			"02.0150", "02.0155", "02.0156"
		}, {
			// *** "Untersuchung durch den Facharzt für Dermatologie"
			"04.0015", "04.0016", "04.0017"
		}, {
			// *** "Vorbesprechung diagnostischer/therapeutischer Eingriffe mit Patienten/Angehörigen durch den Facharzt"
			"00.0050", "00.0055", "00.0056"
		}, {
			// *** "Aktenstudium in Abwesenheit des Patienten"
			"00.0141", "00.0131", "00.0161"
		}, {
			// *** "Erkundigungen bei Dritten in Abwesenheit des Patienten"
			"00.0142", "00.0132", "00.0162"
		}, {
			// *** "Auskünfte an Angehörige oder andere Bezugspersonen des Patienten in Abwesenheit des Patienten"
			"00.0143", "00.0133", "00.0163"
		}, {
			// *** "Besprechungen mit Therapeuten und Betreuern des Patienten in Abwesenheit des Patienten"
			"00.0144", "00.0134", "00.0164"
		}, {
			// *** "Überweisungen an Konsiliarärzte in Abwesenheit des Patienten"
			"00.0145", "00.0135", "00.0165"
		}, {
			// *** "Ausstellen von Rezepten oder Verordnungen ausserhalb von Konsultation, Besuch und telefonischer Konsultation in Abwesenheit des Patienten"
			"00.0146", "00.0136", "00.0166"
		}, {
			// *** "Diagnostische Leistung am Institut für Pathologie/Histologie/Zytologie in Abwesenheit des Patienten"
			"00.0147", "00.0137", "00.0167"
		}, {
			// *** "Tumorboard in Abwesenheit des Patienten"
			"00.0148", "00.0138", "00.0168"
		}, {
			// *** "Telefonische Konsultation durch den Facharzt für Psychiatrie"
			"02.0060", "02.0065", "02.0066"
		}, {
			// *** "Telefonische Konsultation durch behandelnden Psychologen/Psychotherapeuten"
			"02.0150", "02.0155", "02.0156"
		}
	};
	
	public final static String[][][] codeMapListArrays = {
		fiveMinuteChunkCodeMaps, ageGroupLists, ageConnections
	};
	
	public static ArrayList<String> ageConnectionsArray = new ArrayList<String>();
	
	static {
		for (String[] sa : ageConnections)
			// *** add all items except the first one
			for (String s : sa)
				ageConnectionsArray.add(s);
	}
	
	public static ArrayList<String> childrensAdditionsArray = new ArrayList<String>();
	
	static {
		for (String[] sa : childrensAdditions)
			// *** add all items except the first one
			for (String s : sa)
				childrensAdditionsArray.add(s);
	}
	
	/**
	 * contains an array of codes that are simply skipped for optifiying because they are added by
	 * the procs themselves.
	 */
	public static ArrayList<String> skipCodesArray = new ArrayList<String>();
	
	static {
		for (String[][] saa : codeMapListArrays) {
			for (String[] sa : saa)
				// *** add all items except the first one
				for (int i = 1; i < sa.length; i++) {
					String s = sa[i];
					skipCodesArray.add(s);
				}
		}
	}
	
}
