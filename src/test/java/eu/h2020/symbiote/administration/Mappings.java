package eu.h2020.symbiote.administration;

public final class Mappings {

    public static final String VALID_MAPPING = "BASE <http://iosb.fraunhofer.de/ilt/ontologies/educampus#>\n" +
            "PREFIX kit: <http://cm.kit.edu/smartcampus/pim#>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "PREFIX core: <http://www.symbiote-h2020.eu/ontology/core#> \n" +
            "\n" +
            "TRANSFORMATION getFloorName { \"floor level \" + parameters[0].substring(0,1); }\n" +
            "\n" +
            "RULE\n" +
            "   CONDITION\n" +
            "\t  CLASS :BleBeacon\n" +
            "\t\t:beaconId TYPE xsd:string\n" +
            "\tAND :minor TYPE xsd:integer\n" +
            "\tAND :major TYPE xsd:integer\n" +
            "   PRODUCTION \n" +
            "\t  CLASS kit:Beacon\n" +
            "\t\t kit:uuid VALUE REFERENCE :beaconId\n" +
            "\t AND kit:minor VALUE REFERENCE :minor\n" +
            "\t AND kit:major VALUE REFERENCE :major\n" +
            "\t AND core:description VALUE \"mapped beacon from IOSB EduCampus\"\n" +
            "\t\t \n" +
            "\t\t \n" +
            "RULE\n" +
            "\tCONDITION\n" +
            "\t\tCLASS :Room \n" +
            "\t\t\tcore:name TYPE xsd:string\n" +
            "\t\tAND core:description TYPE xsd:string\n" +
            "\t\tAND :capacity TYPE xsd:integer\n" +
            "\t\tAND :roomNo TYPE xsd:string\n" +
            "\tPRODUCTION \n" +
            "\t\t ( CLASS kit:Area\n" +
            "\t\t\tcore:name VALUE REFERENCE core:name\n" +
            "\t\tAND core:description VALUE REFERENCE core:description\n" +
            "\t\tAND kit:roomNo VALUE REFERENCE :roomNo\n" +
            "\t\tAND kit:hasFeature TYPE ( CLASS kit:SeatingCapability\n" +
            "\t\t\t\tkit:capacity VALUE REFERENCE :capacity )\n" +
            "\t\tAND kit:isOnFloor TYPE CLASS kit:Floor\n" +
            "\t\t\t\tcore:name VALUE getFloorName(REFERENCE :roomNo)\n" +
            "\t\t\tAND kit:isInBuilding TYPE CLASS kit:Building\n" +
            "\t\t\t\t\tcore:name VALUE \"Fraunhofer IOSB building Karlsruhe\" )\n" +
            "\n" +
            "RULE\n" +
            "\tCONDITION CLASS :Room\n" +
            "\t\t:hasFeature VALUE :airConditioning\n" +
            "\tPRODUCTION CLASS kit:Area\t\n" +
            "kit:hasFeature TYPE CLASS kit:AirConditioning";

    public static final String INVALID_MAPPING = "Invalid Mapping";

    // Prevent instantiation
    private Mappings() { }
}
