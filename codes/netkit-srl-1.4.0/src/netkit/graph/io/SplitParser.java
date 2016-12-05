/**
 * SplitParser.java
 * Copyright (C) 2008 Sofus A. Macskassy
 *
 * Part of the open-source Network Learning Toolkit
 * http://netkit-srl.sourceforge.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **/

/**
 * $Id$
 **/

package netkit.graph.io;

import java.util.regex.*;
import java.io.*;

/** This class enables parsing lines of text using regular expression
 * patterns that must match an entire line.  The regex is expected to
 * contain "capturing groups", with at least one such group for each
 * expected field in the line.  You can either make use of the
 * supplied parsers and their respective patterns or subclass this
 * object and define your own patterns and captured group extraction
 * method (if necessary).  This class aims to be faster than and
 * create less garbage than using Pattern.split() on large input
 * files.
 *
 * @see java.util.regex.Pattern
 * @see java.util.regex.Matcher
 *
 * @author Kaveh R. Ghazi
 */
public abstract class SplitParser
{
	/** A String array used for returning the parsed input.  The
	 * captured regex groups are placed into this array.  This array
	 * is reused for each line to avoid creating excess garbage for
	 * the collector, so clients must extract the elements and store
	 * them elsewhere before parsing the next line.  Access to this
	 * field is made available to subclasses which elect to override
	 * the default {@link #parseLine(CharSequence)} method.
	 */
	protected final String[] fields;

	/** The Matcher object used to parse each line.  It contains the
	 * Pattern and regex used for matching.  Access to this field is
	 * made available to subclasses which elect to override the
	 * default {@link #parseLine(CharSequence)} method.
	 */
	protected final Matcher matcher;

	// Parses whitespace separated values, separators are exactly one
	// character and no extra whitespace appears anywhere.
	private static final class SplitParserWS1 extends SplitParser
	{
		private SplitParserWS1(int fieldNum)
		{
			super(fieldNum, "^(\\S+)", "\\s(\\S+)", "$");
		}
	}

	// Parses whitespace separated values, with arbitrary extra
	// whitespace between values.
	private static final class SplitParserWS extends SplitParser
	{
		private SplitParserWS(int fieldNum)
		{
			super(fieldNum, "^\\s*(\\S+)", "\\s+(\\S+)", "\\s*$");
		}
	}

	// Parses comma separated tokens, whitespace is not elided.
	private static final class SplitParserCOMMA extends SplitParser
	{
		private SplitParserCOMMA(int fieldNum)
		{
			super(fieldNum, "^([^,]+)", ",([^,]+)", "$");
		}
	}

	// Parses comma separated values, possibly surrounded with
	// whitespace which is elided.
	private static final class SplitParserCOMMAWS extends SplitParser
	{
		private SplitParserCOMMAWS(int fieldNum)
		{
			super(fieldNum, "^\\s*([^,\\s]+)\\s*", ",\\s*([^,\\s]+)\\s*", "$");
		}
	}

	// Parses comma separated values possibly wrapped with double
	// quotes, possibly surrounded with whitespace.  This allows
	// intentional whitespace or commas in the values and is a more
	// robust parser for CSV representation.
	private static final class SplitParserCSV extends SplitParser
	{
		private SplitParserCSV(int fieldNum)
		{
			super(fieldNum, "^\\s*(?:\"([^\"]*)\"|([^,\\s]*))\\s*",
					",\\s*(?:\"([^\"]*)\"|([^,\\s]*))\\s*", "$");
		}

		// Overrides the superclass method because each field has two
		// possible regex groups "(?:()|())", either of which may
		// contain values which we need to grab.  Note the containing
		// group is non-capturing.
		public String[] parseLine(CharSequence line)
		{
			matcher.reset(line);
			if (matcher.find())
			{
				for (int i=fields.length; i-->0; )
				{
					String match;
					if ((match = matcher.group((i+1)*2)) != null)
						fields[i] = match;
					else if ((match = matcher.group((i+1)*2-1)) != null)
						fields[i] = match;
					else
						throw new RuntimeException("Got a null field in <"+line+">");
				}
			}
			else	    
				throw new RuntimeException("Didn't find a match in <"+line+">");
			return fields;
		}
	}

	/** The protected constructor which creates the field holder array
	 * and assembles the regular expression for matching lines.
	 * Subclasses must supply to this the number of fields expected in
	 * each line, and the pieces to construct the regex.
	 * @param fieldNum and int specifying the number of fields in each
	 * line of parsed text.
	 * @param patternStart a String which begins the regex pattern; it
	 * normally contains the capturing group for the first field.
	 * @param patternMiddle a String which is appended fieldNum-1
	 * times after the patternStart in the regex pattern; it normally
	 * contains a capturing group used for any fields after the first.
	 * @param patternEnd a String which is appended to the end of the
	 * regex pattern; it normally contains any regex suffix necessary
	 * to terminate the expression.
	 */
	protected SplitParser(int fieldNum, String patternStart,
			String patternMiddle, String patternEnd)
	{
		this.fields = new String[fieldNum];

		final StringBuilder pattern = new StringBuilder(patternStart);
		for (int i=1; i<fieldNum; i++)
			pattern.append(patternMiddle);
		pattern.append(patternEnd);
		this.matcher = Pattern.compile(pattern.toString()).matcher("");
	}

	/** Parses a line of text according to the regex defined for this
	 * parser and splits it into an array of String.  This default
	 * implementation assumes the regex has one matched "group"
	 * captured per expected field in the supplied line of text.
	 * Override this if the pattern has multiple regex groups per
	 * field and extract them accordingly.
	 * @param line a CharSequence containing the line of text to
	 * split.
	 * @return an array of String containing the matching fields from
	 * the supplied line of text; this array is reused for each call
	 * to this method.
	 * @throws RuntimeException if the supplied line doesn't match
	 * this parser's regex.
	 */
	public String[] parseLine(CharSequence line)
	{
		matcher.reset(line);
		if (matcher.find())
		{
			for (int i=fields.length; --i>=0; )
				if ((fields[i] = matcher.group(i+1)) == null)
					throw new RuntimeException("Got a null field in <"+line+">");
		}
		else
			throw new RuntimeException("Didn't find a match in <"+line+">");
		return fields;
	}

	/** Gets a String representation of the regular expression defined
	 * for the Matcher object used on each line.  The expression will
	 * be specific to the number of fields defined for this parser
	 * object.  This is mainly used to verify the constructed
	 * expression if desired.
	 * @return a String representation of the regular expression
	 * defined for the Matcher object used on each line.
	 */
	public final String getRegex()
	{
		return matcher.pattern().pattern();
	}

	/** Gets a parser that parses lines containing whitespace
	 * separated values, separators are exactly one character and no
	 * extra whitespace appears anywhere in the supplied lines.
	 * @param fieldNum the number of fields expected per line.
	 * @return a parser that parses lines containing whitespace
	 * separated values.
	 */
	static final public SplitParser getParserWS1(int fieldNum)
	{
		return new SplitParserWS1(fieldNum);
	}

	/** Gets a parser that parses lines containing whitespace
	 * separated values, with arbitrary extra whitespace between
	 * values.
	 * @param fieldNum the number of fields expected per line.
	 * @return a parser that parses lines containing whitespace
	 * separated values, with arbitrary extra whitespace between
	 * values.
	 */
	static final public SplitParser getParserWS(int fieldNum)
	{
		return new SplitParserWS(fieldNum);
	}

	/** Gets a parser that parses lines containing comma separated
	 * values, no whitespace allowed.
	 * @param fieldNum the number of fields expected per line.
	 * @return a parser that parses lines containing comma separated
	 * values, no whitespace allowed.
	 */
	static final public SplitParser getParserCOMMA(int fieldNum)
	{
		return new SplitParserCOMMA(fieldNum);
	}

	/** Gets a parser that parses lines containing comma separated
	 * values, possibly surrounded with whitespace; whitespace is
	 * ignored/removed.
	 * @param fieldNum the number of fields expected per line.
	 * @return a parser that parses lines containing comma separated
	 * values.
	 */
	static final public SplitParser getParserCOMMAWS(int fieldNum)
	{
		return new SplitParserCOMMAWS(fieldNum);
	}

	/** Gets a parser that parses lines containing comma separated
	 * values possibly wrapped with double quotes, possibly surrounded
	 * with whitespace.  This allows whitespace in the values and is a
	 * more accurate CSV format.
	 * @param fieldNum the number of fields expected per line.
	 * @return a parser that parses lines containing comma separated
	 * values possibly wrapped with double quotes, possibly surrounded
	 * with whitespace.
	 */
	static final public SplitParser getParserCSV(int fieldNum)
	{
		return new SplitParserCSV(fieldNum);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			final SplitParser sp = SplitParser.getParserCSV(10);
			final String testString
			= "  \"LU\"  ,  86.25  ,  \"11/4/1998\" , \"2:19PM\" , , , +4.0625 , \"\" , \" \"  ,  \"Hello, World!\"";

			for (final String s : sp.parseLine(testString))
				System.err.print("<"+s+">");
			System.err.println();
		}
		else
		{
			try
			{
				//final SplitParser sp = SplitParser.getParserWS(3);
				final SplitParser sp = SplitParser.getParserWS1(3);
				//final SplitParser sp = SplitParser.getParserCOMMA(2);
				//final SplitParser sp = SplitParser.getParserCOMMAWS(3);
				//final SplitParser sp = SplitParser.getParserCOMMA(3);
				System.err.println("Regex: " + sp.getRegex());

				final BufferedReader br = new BufferedReader(new FileReader(args[0]));
				for (String line = br.readLine(); line != null; line = br.readLine())
				{
					if (false)
					{
						//for (final String s : pattern.split(line))
						for (final String s : sp.parseLine(line))
							System.err.print("<"+s+">");
						System.err.println();
					}
					else
						sp.parseLine(line);
					//pattern.split(line);
				}
				br.close();
			}
			catch (IOException ioe)
			{
				System.err.println(ioe);
			}
		}
	}
}
