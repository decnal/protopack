package com.risenperspectives.pgparsers;

import java.util.ArrayList;
import java.util.List;

public class TestFlatPGP {

    public static void main(String[] args) {
        List<String> tests = new ArrayList<>();
        tests.add("REQ.G1=g1\nREQ.G2=g2");
        tests.add("REQ.G1.E1=ell\nREQ.G1.E2=e12");
        tests.add("root.child=value");
        tests.add("root.parent.child=value");
        tests.add("REQ.G1.E1.E11=ell");
        tests.add("REQ.G1.E1.E11=ell\nREQ.G1.E1.E12=e12\nREQ.G1.E21=e21");
        tests.add("root.child#aTTr=value\nroot.child=text");
        tests.add("name.subname.[0]=true\n"
			+ "name.subname.[0].indx0=value0\n"
			+ "name.subname.[1].indx1=value1\n"
			+ "name.subname.[2].indx2=value2\n"
		+ "name.subname.[2]=100.23\n"
		+ "name.subname.[3]=three\n"
		+ "name.subname.[3].sub.[0]=three-zero\n"
		+ "name.subname.[3].sub.[1]=three-one\n"
		);
        tests.add("");
        //tests.add("<root><child></root>");
        final int N = 10;
        StringBuilder sb = new StringBuilder(N*20+10);
        for (int i = 0; i < N; i++) {
            sb.append("root.child.["+i+"]=" + i);
        }
        sb.append("root");
        tests.add(sb.toString());
        tests.add("root#xmlns:ns=http://example.com\nroot.ns:child=value</ns:child></root>");
        tests.add("root=some <cdata> content");

        runTests(tests, true, true, true, true);
    }

    private static void runTests(List<String> tests, boolean runJson, boolean runYaml, boolean runXml, boolean runFlat) {
        FlatPGParser flatParser = new FlatPGParser();
        FlatPGPSerializer flatSerializer = new FlatPGPSerializer();
        XmlPGPSerializer xmlSerializer = new XmlPGPSerializer();
        JsonPGPSerializer jsonSerializer = new JsonPGPSerializer();
        YamlPGPSerializer yamlSerializer = new YamlPGPSerializer();

        for (String test : tests) {
            try {
                System.out.println("Test String: " + test);

                PGPNode topNode = flatParser.parse(test);

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
}//TestFlatPGP
