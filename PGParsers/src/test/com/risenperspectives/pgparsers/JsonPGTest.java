package com.risenperspectives.pgparsers;

import java.util.ArrayList;
import java.util.List;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class JsonPGTest {

	public static void main(String[] args) {
		List<String> tests = new ArrayList<>();
		tests.add("{ n: x, a: {b: i} }");
		tests.add("{ \"name\" : \" \\\"Q\\\" N-\\n R-\\r T-\\t F-\\f B-\\b \" }");
		tests.add("{name: value}");
		tests.add("{name:two: value} #ignore this comment\n#and ignore this comment");
		tests.add("{name:value}");
		tests.add("{name:{subname: subvalue,subname2: subvalue2}}");
		tests.add("{name:{subname:subvalue,subname2:subvalue2}}");
		tests.add("{'name':{'subname':'subvalue','subname2':'subvalue2'}}");
		tests.add("{name:{subname: subvalue,subname2: subvalue2}}");
		tests.add("{name:{subname:[true,{indx2: value2},100.23]}}");
		tests.add("{num_array:[12.34,12E43,-0.56e-78]}");
		tests.add("{ num_array: [\n    12.34,\n    12E43,\n    -0.56e-78\n    ]\n  }");
		tests.add("{ types: { \"boolean\" : [true,false], \"null\":null, \"quoted\": \"in  N-\\n R-\r T-\t F-\f B-\b end\", \"number\": [0, 0.1, 1.23, 1e10, -1, -1.2, 1.2e-3] }}");
		tests.add("{ multi-array: [[11,12,[23,24,[35,36],27],18,[29],[],11]]}");
		tests.add("{ all_true: [y,Y,yes,Yes,YES,on,On,ON,true,True,TRUE] }");
		tests.add("{ all_false: [n,N,no,No,NO,off,Off,OFF,false,False,FALSE] }");
		tests.add("{ array: [ &AB ALIAS, XY, *AB ] }");
		tests.add("{ a1: &CD [1,2,3], a2: *CD }");
		tests.add("{ a1: &a1 [1,2,3], a2: &a2 [2,4,6], a12: [*a1 ,*a2 ] }");
		tests.add("{ new_test: { nested: { array: [1, 2, 3], object: { key: value } } } }");
		tests.add("{ another_test: { list: [true, false, null], number: 12345 } }");

		runTests(tests, true, true, true);
	}

	private static void runTests(List<String> tests, boolean runJson, boolean runYaml, boolean runXml) {
		JsonPGParser jsonParser = new JsonPGParser();
		JsonPGPSerializer jsonSerializer = new JsonPGPSerializer();
		YamlPGPSerializer yamlSerializer = new YamlPGPSerializer();
		XmlPGParser xmlParser = new XmlPGParser();
		XmlPGPSerializer xmlSerializer = new XmlPGPSerializer();

		for (String test : tests) {
			try {
				System.out.println("Test String: " + test);

				PGPNode topNode = jsonParser.parse(test);

				if (runJson) {
					runSerializer(jsonSerializer, topNode );
				}
				if (runYaml) {
					runSerializer(yamlSerializer, topNode );
				}
				if (runXml) {
					runSerializer(xmlSerializer, topNode );
				}

				topNode.delete();
			} catch (PGPException e) {
				System.out.println("PGPException at offset " + e.getErrorCharOfLine() + " of line " + e.getErrorLine() + ": " + e.getMessage());
			}
		}
	}

	private static void runSerializer(PGPSerializerInterface serializer, PGPNode topNode ) {
		StringBuilder sb;

		serializer.setDepthSpaces(0);
		serializer.setDebugLevel(0);
		sb = serializer.serialize( new StringBuilder(), topNode, new PGPOptionFlags() );
		System.out.println("as " + serializer.protocolName() + " space=" + serializer.getDepthSpaces() + ", Flags=" + serializer.getFlags().getFlagsInt());
		System.out.println(sb.toString());

		serializer.setDepthSpaces(2);
		serializer.setDebugLevel(0);
		sb = serializer.serialize( new StringBuilder(), topNode, new PGPOptionFlags() );
		System.out.println("as " + serializer.protocolName() + " space=" + serializer.getDepthSpaces() + ", Flags=" + serializer.getFlags().getFlagsInt());
		System.out.println(sb.toString());
	}
}
