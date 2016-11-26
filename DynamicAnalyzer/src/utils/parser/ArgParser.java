package utils.parser;

import org.apache.commons.cli.*;

import java.io.File;
import java.nio.file.*;

import utils.exceptions.IllegalArgumentsException;

public class ArgParser {

	public static void printHelp() {
		System.out.println(" ====== POSSIBLE FLAGS =======");
		System.out.println("-j: Compile to Jimple. ");
		System.out.println("-p: Set a different input file ");
		System.out.println("-o: Set output folder. ");
		System.out.println("\nExamples:");
		System.out.println("main.testclasses.NSUPolicy1");
		System.out.println("main.testclasses.NSUPolicy1 -j");
		System.out
				.println("main.testclasses.NSUPolicy1 -o /Users/NicolasM/myOutputFolder");
		System.out
				.println("main.testclasses.NSUPolicy1 -p /Users/NicolasM/Downloads/Users/NicolasM/Downloads");
		System.out
				.println("main.testclasses.NSUPolicy1 -p /Users/NicolasM/Downloads/Users/NicolasM/Downloads -j");
	}

	public static String[] getSootOptions(String[] args) {

		if (args[0].startsWith("-")) {
			throw new IllegalArgumentsException("first argument must be the main Class!");
		}

		Options options = new Options();

		Option pathToMainclass = new Option("p", "path", true,
				"Optinal: Path to MainClass");
		pathToMainclass.setRequired(false);
		options.addOption(pathToMainclass);

		Option output = new Option("o", "output", true, "output file");
		output.setRequired(false);
		options.addOption(output);

		Option format = new Option("j", "jimple", false,
				"output as Jimple instead of as compiled class");
		format.setRequired(false);
		options.addOption(format);

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd;
		try {

			cmd = parser.parse(options, args);

			// template for the string we will return to soot. also use 4th and
			// 7th element for ant (see bottom of Main.main)
			String[] template = new String[] { "-f", "c", "-main-class",
					"path/to/file placeholder", "mainclass placeholder", "--d",
					"placeholder output path" };

			// case no flag
			template[3] = args[0]; // set mainclass
			template[4] = args[0];
			template[6] = System.getProperty("user.dir");

			// case j flag
			if (cmd.hasOption("j")) {
				template[1] = "J";
			}

			// case p flag
			if (cmd.hasOption("p")) {
				File path = new File(cmd.getOptionValue("p"));
				template[3] = args[0]; // set mainclass

				if (path.isAbsolute()) {
					template[4] = args[0];
				} else {
					File parent = new File(System.getProperty("user.dir"));
					File fullPath = new File(parent, cmd.getOptionValue("p"));
					template[4] = fullPath.getAbsolutePath(); // set path to
																// file
				}
			}

			// case o flag
			if (cmd.hasOption("o")) {
				File out = new File(cmd.getOptionValue("o"));

				if (out.isAbsolute()) {
					template[6] = out.getAbsolutePath();
				} else {
					File parent = new File(System.getProperty("user.dir"));
					File fullPath = new File(parent, cmd.getOptionValue("o"));
					template[6] = fullPath.getAbsolutePath();
				}
			}

			return template;

			// if illegal input
		} catch (ParseException e) {
			throw new IllegalArgumentsException(e.getMessage());
		}
	}
}
