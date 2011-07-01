/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi;

import org.eclipse.equinox.bidi.custom.STextFeatures;
import org.eclipse.equinox.bidi.custom.STextProcessor;

/**
 *  This class provides a number of convenience functions facilitating the
 *  processing of structured text.
 *
 *  @noextend This class is not intended to be subclassed by clients.
 *  @noinstantiate This class is not intended to be instantiated by clients.
 *
 *  @author Matitiahu Allouche
 */
final public class STextUtil {

	/**
	 *  prevent instantiation
	 */
	private STextUtil() {
		// empty
	}

	/** This is a convenience method which can add directional marks in a given
	 *  text before the characters specified in the given array of offsets,
	 *  and can add a prefix and/or a suffix of directional formatting characters.
	 *  This can be used for instance after obtaining offsets by calling
	 *  {@link STextEngine#leanBidiCharOffsets leanBidiCharOffsets} in order to
	 *  produce a <i>full</i> text corresponding to the source text.
	 *  The directional formatting characters that will be added at the given
	 *  offsets will be LRMs for structured text strings with LTR base direction
	 *  and RLMs for strings with RTL base direction. Leading and
	 *  trailing LRE, RLE and PDF which might be needed as prefix or suffix
	 *  depending on the orientation of the GUI component used for display
	 *  may be added depending on argument <code>affix</code>.
	 *
	 *  @param  text is the structured text string
	 *
	 *  @param  offsets is an array of offsets to characters in <code>text</code>
	 *          before which an LRM or RLM will be inserted.
	 *          Members of the array must be non-negative numbers smaller
	 *          than the length of <code>text</code>.
	 *          The array must be sorted in ascending order without duplicates.
	 *          This argument may be null if there are no marks to add.
	 *
	 *  @param  direction specifies the base direction of the structured text.
	 *          It must be one of the values {@link STextFeatures#DIR_LTR} or
	 *          {@link STextFeatures#DIR_RTL}.
	 *
	 *  @param  affix specifies if a prefix and a suffix should be added to
	 *          the result to make sure that the <code>direction</code>
	 *          specified as third argument is honored even if the string
	 *          is displayed in a GUI component with a different orientation.
	 *
	 *  @return a string corresponding to the source <code>text</code> with
	 *          directional marks (LRMs or RLMs) added at the specified offsets,
	 *          and directional formatting characters (LRE, RLE, PDF) added
	 *          as prefix and suffix if so required.
	 */
	public static String insertMarks(String text, int[] offsets, int direction, boolean affix) {
		int textLen = text.length();
		if (textLen == 0)
			return ""; //$NON-NLS-1$

		String curPrefix, curSuffix, full;
		char curMark, c;
		char[] fullChars;
		if (direction == STextFeatures.DIR_LTR) {
			curMark = LRM;
			curPrefix = "\u202a\u200e"; /* LRE+LRM *///$NON-NLS-1$
			curSuffix = "\u200e\u202c"; /* LRM+PDF *///$NON-NLS-1$
		} else {
			curMark = RLM;
			curPrefix = "\u202b\u200f"; /* RLE+RLM *///$NON-NLS-1$
			curSuffix = "\u200f\u202c"; /* RLM+PDF *///$NON-NLS-1$
		}
		// add marks at offsets
		if ((offsets != null) && (offsets.length > 0)) {
			int offLen = offsets.length;
			fullChars = new char[textLen + offLen];
			int added = 0;
			for (int i = 0, j = 0; i < textLen; i++) {
				c = text.charAt(i);
				if ((j < offLen) && (i == offsets[j])) {
					fullChars[i + added] = curMark;
					added++;
					j++;
				}
				fullChars[i + added] = c;
			}
			full = new String(fullChars);
		} else {
			full = text;
		}
		if (affix)
			return curPrefix + full + curSuffix;
		return full;
	}

	/*************************************************************************/
	/*                                                                       */
	/*  The following code is provided for compatibility with TextProcessor  */
	/*                                                                       */
	/*************************************************************************/

	//  The default set of separators to use to segment a string.
	private static final String defaultSeparators = ".:/\\"; //$NON-NLS-1$
	// left to right mark
	private static final char LRM = '\u200e';
	// left to right mark
	private static final char RLM = '\u200f';
	// left to right embedding
	private static final char LRE = '\u202a';
	// right to left embedding
	private static final char RLE = '\u202b';
	// pop directional format
	private static final char PDF = '\u202c';

	static boolean isProcessingNeeded() {
		if (!STextEnvironment.isSupportedOS())
			return false;
		return STextEnvironment.DEFAULT.isBidi();
	}

	/**
	 *  Process the given text and return a string with appropriate
	 *  directional formatting characters if the locale is a bidi locale.
	 *  This is equivalent to calling
	 *  {@link #process(String str, String separators)} with the default
	 *  set of separators (dot, colon, slash, backslash).
	 *
	 *  @param  str the text to be processed.
	 *
	 *  @return the processed string.
	 */
	public static String process(String str) {
		return process(str, defaultSeparators);
	}

	/**
	 *  Process a string that has a particular semantic meaning to render
	 *  it correctly on bidi locales. This is done by adding directional
	 *  formatting characters so that presentation using the Unicode
	 *  Bidirectional Algorithm will provide the expected result.
	 *  The text is segmented according to the provided separators.
	 *  Each segment has the Unicode Bidi Algorithm applied to it,
	 *  but as a whole, the string is oriented left to right.
	 *  <p>
	 *  For example, a file path such as <tt>d:\myfolder\FOLDER\MYFILE.java</tt>
	 *  (where capital letters indicate RTL text) should render as
	 *  <tt>d:\myfolder\REDLOF\ELIFYM.java</tt>.</p>
	 *  <p>
	 *  NOTE: this method inserts directional formatting characters into the
	 *  text. Methods like <code>String.equals(String)</code> and
	 *  <code>String.length()</code> called on the resulting string will not
	 *  return the same values as would be returned for the original string.</p>
	 *
	 *  @param  str the text to process.
	 *
	 *  @param  separators separators by which the string will be segmented.
	 *          If <code>null</code>, the default separators are used
	 *          (dot, colon, slash, backslash).
	 *
	 *  @return the processed string.
	 *          If <code>str</code> is <code>null</code>,
	 *          or of length 0, or if the current locale is not a bidi one,
	 *          return the original string.
	 */
	public static String process(String str, String separators) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		// do not process a string that has already been processed.
		if (str.charAt(0) == LRE && str.charAt(str.length() - 1) == PDF)
			return str;

		// do not process a string if all the following conditions are true:
		//  a) it has no RTL characters
		//  b) it starts with a LTR character
		//  c) it ends with a LTR character or a digit
		boolean isStringBidi = false;
		int strLength = str.length();
		char c;
		for (int i = 0; i < strLength; i++) {
			c = str.charAt(i);
			if (((c >= 0x05d0) && (c <= 0x07b1)) || ((c >= 0xfb1d) && (c <= 0xfefc))) {
				isStringBidi = true;
				break;
			}
		}
		while (!isStringBidi) {
			if (!Character.isLetter(str.charAt(0)))
				break;
			c = str.charAt(strLength - 1);
			if (!Character.isDigit(c) && !Character.isLetter(c))
				break;
			return str;
		}

		if (separators == null)
			separators = defaultSeparators;

		// make sure that LRE/PDF are added around the string
		STextEnvironment env = new STextEnvironment(null, false, STextEnvironment.ORIENT_UNKNOWN);
		STextFeatures features = new STextFeatures(separators, 0, -1, -1, false, false);
		return STextEngine.leanToFullText(new STextProcessor(), features, env, str, null);
	}

	/**
	 *  Process a string that has a particular semantic meaning to render
	 *  it correctly on bidi locales. This is done by adding directional
	 *  formatting characters so that presentation using the Unicode
	 *  Bidirectional Algorithm will provide the expected result..
	 *  The text is segmented according to the syntax specified in the
	 *  <code>type</code> argument.
	 *  Each segment has the Unicode Bidi Algorithm applied to it, but the
	 *  order of the segments is governed by the type of the structured text.
	 *  <p>
	 *  For example, a file path such as <tt>d:\myfolder\FOLDER\MYFILE.java</tt>
	 *  (where capital letters indicate RTL text) should render as
	 *  <tt>d:\myfolder\REDLOF\ELIFYM.java</tt>.</p>
	 *  <p>
	 *  NOTE: this method inserts directional formatting characters into the
	 *  text. Methods like <code>String.equals(String)</code> and
	 *  <code>String.length()</code> called on the resulting string will not
	 *  return the same values as would be returned for the original string.</p>
	 *
	 *  @param  str the text to process.
	 *
	 *  @param  type specifies the type of the structured text. It must
	 *          be one of the values in {@link ISTextTypes} or a value.
	 *          added by a plug-in extension.
	 *
	 *  @return the processed string.
	 *          If <code>str</code> is <code>null</code>,
	 *          or of length 0, or if the current locale is not a bidi one,
	 *          return the original string.
	 */
	public static String processTyped(String str, String type) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		// do not process a string that has already been processed.
		char c = str.charAt(0);
		if (((c == LRE) || (c == RLE)) && str.charAt(str.length() - 1) == PDF)
			return str;

		// make sure that LRE/PDF are added around the string
		STextEnvironment env = new STextEnvironment(null, false, STextEnvironment.ORIENT_UNKNOWN);
		return STextEngine.leanToFullText(type, null, env, str, null);
	}

	/**
	 *  Remove directional formatting characters in the given string that
	 *  were inserted by one of the {@link #process process} methods.
	 *
	 *  @param  str string with directional characters to remove.
	 *
	 *  @return string with no directional formatting characters.
	 */
	public static String deprocess(String str) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		StringBuffer buf = new StringBuffer();
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			char c = str.charAt(i);
			switch (c) {
				case LRM :
					continue;
				case LRE :
					continue;
				case PDF :
					continue;
				default :
					buf.append(c);
			}
		}
		return buf.toString();
	}

	/**
	 *  Remove directional formatting characters in the given string that
	 *  were inserted by the {@link #processTyped processTyped} method.
	 *
	 *  @param  str string with directional characters to remove.
	 *
	 *  @param  type type of the structured text as specified when
	 *          calling {@link #processTyped processTyped}.
	 *
	 *  @return string with no directional formatting characters.
	 */
	public static String deprocess(String str, String type) {
		if ((str == null) || (str.length() <= 1) || !isProcessingNeeded())
			return str;

		// make sure that LRE/PDF are added around the string
		STextEnvironment env = new STextEnvironment(null, false, STextEnvironment.ORIENT_UNKNOWN);
		return STextEngine.fullToLeanText(type, null, env, str, null);
	}

}