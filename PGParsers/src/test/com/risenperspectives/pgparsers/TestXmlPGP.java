package com.risenperspectives.pgparsers;

import java.util.ArrayList;
import java.util.List;

public class TestXmlPGP {

    public static void main(String[] args) {
        List<String> tests = new ArrayList<>();
        tests.add("<root><child>value</child></root>");
        tests.add("<root><parent><child>value</child></parent></root>");
        tests.add("<REQ><G1><E1><E11>e11</E11><E12>e12</E12></E1></G1><G2><E21>e21</E21></G2></REQ>");
        tests.add("<root><child aTTr=\"value\">text</child></root>");
        tests.add("");
        tests.add("<root><child></root>");
        final int N = 10;
        StringBuilder sb = new StringBuilder(N*20+10);
        sb.append("<root>");
        for (int i = 0; i < N; i++) {
            sb.append("<child>" + i + "</child>");
        }
        sb.append("</root>");
        tests.add(sb.toString());
        tests.add("<root xmlns:ns=\"http://example.com\"><ns:child>value</ns:child></root>");
        tests.add("<root><![CDATA[some <cdata> content]]></root>");

        runTests(tests, true, true, true, true);
    }

    private static void runTests(List<String> tests, boolean runJson, boolean runYaml, boolean runXml, boolean runFlat) {
        XmlPGParser xmlParser = new XmlPGParser();
        XmlPGPSerializer xmlSerializer = new XmlPGPSerializer();
        JsonPGPSerializer jsonSerializer = new JsonPGPSerializer();
        YamlPGPSerializer yamlSerializer = new YamlPGPSerializer();
        FlatPGPSerializer flatSerializer = new FlatPGPSerializer();

        for (String test : tests) {
            try {
                System.out.println("Test String: " + test);

                PGPNode topNode = xmlParser.parse(test);

                if (runXml) {
                    runSerializer(xmlSerializer, topNode, 0 );
                    runSerializer(xmlSerializer, topNode, 2 );
                }
                if (runJson) {
                    runSerializer(jsonSerializer, topNode, 0 );
                    runSerializer(jsonSerializer, topNode, 2 );
                }
                if (runYaml) {
                    runSerializer(yamlSerializer, topNode, 2 );
                }
                if (runFlat) {
                    runSerializer(flatSerializer, topNode, 0 );
                }

                topNode.delete();
            } catch (PGPException e) {
                System.out.println("PGPException at offset " + e.getErrorCharOfLine() + " of line " + e.getErrorLine() + ": " + e.getMessage());
            }
        }
    }

    private static void runSerializer(PGPSerializerInterface serializer, PGPNode topNode, int depthSpaces) {
        StringBuilder sb;

        serializer.setDepthSpaces(depthSpaces);
        serializer.setDebugLevel(0);
        sb = serializer.serialize( new StringBuilder(), topNode, new PGPOptionFlags() );
        System.out.println("as " + serializer.protocolName() + " space=" + serializer.getDepthSpaces() + ", Flags=" + serializer.getFlags().getFlagsInt());
        System.out.println(sb.toString());
    }
}
