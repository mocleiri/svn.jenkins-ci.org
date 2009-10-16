package hudson.plugins.chucknorris;

import java.util.Random;

public class FactGenerator {

	// source:
	//http://www.codesqueeze.com/the-ultimate-top-25-chuck-norris-the-programmer
	// -jokes/
	private static final String[] FACTS = {
			"All arrays Chuck Norris declares are of infinite size, because Chuck Norris knows no bounds.",
			"Chuck Norris doesn't have disk latency because the hard drive knows to hurry the hell up.",
			"All browsers support the hex definitions #chuck and #norris for the colors black and blue.",
			"Chuck Norris can't test for equality because he has no equal.",
			"Chuck Norris doesn't need garbage collection because he doesn't call .Dispose(), he calls .DropKick().",
			"Chuck Norris's first program was kill -9.",
			"Chuck Norris burst the dot com bubble.",
			"Chuck Norris writes code that optimizes itself.",
			"Chuck Norris can write infinite recursion functions... and have them return.",
			"Chuck Norris can solve the Towers of Hanoi in one move.",
			"The only pattern Chuck Norris knows is God Object.",
			"Chuck Norris finished World of Warcraft.",
			"Project managers never ask Chuck Norris for estimations... ever.",
			"Chuck Norris doesn't use web standards as the web will conform to him.",
			"\"It works on my machine\" always holds true for Chuck Norris.",
			"Whiteboards are white because Chuck Norris scared them that way.",
			"Chuck Norris's beard can type 140 wpm.",
			"Chuck Norris can unit test an entire application with a single assert.",
			"Chuck Norris doesn't bug hunt as that signifies a probability of failure, he goes bug killing.",
			"Chuck Norris's keyboard doesn't have a Ctrl key because nothing controls Chuck Norris.",
			"Chuck Norris doesn't need a debugger, he just stares down the bug until the code confesses.",
			"Chuck Norris can access private methods.",
			"Chuck Norris can instantiate an abstract class.",
			"Chuck Norris doesn'tt need to know about class factory pattern. He can instantiate interfaces.",
			"The class object inherits from Chuck Norris.",
			"For Chuck Norris, NP-Hard = O(1).",
			"Chuck Norris knows the last digit of PI.",
			"Chuck Norris can divide by zero.",
			"Chuck Norris doesn't get compiler errors, the language changes itself to accommodate Chuck Norris.",
			"The programs that Chuck Norris writes don't have version numbers because he only writes them once. If a user reports a bug or has a feature request they don't live to see the sun set.",
			"Chuck Norris doesn't believe in floating point numbers because they can't be typed on his binary keyboard.",
			"Chuck Norris solved the Travelling Salesman problem in O(1) time.",
			"Chuck Norris never gets a syntax error. Instead, The language gets a DoesNotConformToChuck error.",
			"No statement can catch the ChuckNorrisException.",
			"Chuck Norris doesn't program with a keyboard. He stares the computer down until it does what he wants.",
			"Chuck Norris doesn't pair program.",
			"Chuck Norris can write multi-threaded applications with a single thread.",
			"There is no Esc key on Chuck Norris' keyboard, because no one escapes Chuck Norris.",
			"Chuck Norris doesn't delete files, he blows them away.",
			"Chuck Norris can binary search unsorted data.",
			"Chuck norris breaks RSA 128-bit encrypted codes in milliseconds.",
			"Chuck Norris went out of an infinite loop.",
			"Chuck Norris can read all encrypted data, because nothing can hide from Chuck Norris.",
			"Chuck Norris hosting is 101% uptime guaranteed.",
			"When a bug sees Chuck Norris, it flees screaming in terror, and then immediately self-destructs to avoid being roundhouse-kicked.",
			"Chuck Norris rewrote the Google search engine from scratch.",
			"Chuck Norris doesn't need the cloud to scale his applications, he uses his laptop.",
			"Chuck Norris can access the DB from the UI.",
			"Chuck Norris protocol design method has no status, requests or responses, only commands.",
			"Chuck Norris programs occupy 150% of CPU, even when they are not executing.",
			"Chuck Norris can spawn threads that complete before they are started.",
			"Chuck Norris programs do not accept input.",
			"Chuck Norris doesn't need an OS.",
			"Chuck Norris can compile syntax errors.",
			"Chuck Norris compresses his files by doing a flying round house kick to the hard drive.",
			"Chuck Norris doesn't use a computer because a computer does everything slower than Chuck Norris." };

	private static final Random RANDOM = new Random();;

	public String random() {
		return FACTS[RANDOM.nextInt(FACTS.length)];
	}
}
