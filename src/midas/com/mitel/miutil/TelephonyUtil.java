/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mitel.miutil;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class utility containing useful telephony-related functions
 * @author haiv
 */
public class TelephonyUtil
{
	private static final Pattern gDnTokens = Pattern.compile("([0-9]+|[\\#\\*]+)");
	private static final Pattern gNumerical = Pattern.compile("[0-9]+");
	/**
	 * Given a DN, splits it up into an array of tokens of either numerical or "#*" types.
	 * @param dn The DN to be tokenised
	 * @return  The array of tokens from the given DN, may be empty but never null.
	 */
	private static Object[] tokenise(String dn) {
		List<String> tokens = new LinkedList<String>();
		Matcher m = gDnTokens.matcher(dn);
		while(m.find()) {
			tokens.add(m.group(1));
		}
		return tokens.toArray();
	}
	/**
	 * Given an array of DN tokens determine the index of the least significant (right most) numerical token.
	 * @param tokens The tokens to be examined
	 * @return The index to the least significant numerical token, or -1 if no numerical token is present.
	 */
	private static int leastSignificantNumericalIndex(Object[] tokens) {
		int ret = -1;
		if(tokens != null) {
			for(int idx = 0; idx < tokens.length; ++idx) {
				if(gNumerical.matcher(tokens[idx].toString()).find()) {
					ret = idx;
				}
			}
		}
		return ret;
	}
	/**
	 * Validates the DN range
	 * @param lowerTokens The tokens for the lower limit DN
	 * @param upperTokens The tokens for the upper limit DN
	 * @throws com.mitel.miutil.MiException The given range is invalid
	 */
	private static void validateRange(Object[] lowerTokens, Object[] upperTokens) throws MiException {
		if(lowerTokens.length != upperTokens.length) {
			throw new MiException("Different number of tokens");
		}
		int lowerLsnIdx = leastSignificantNumericalIndex(lowerTokens);
		int upperLsnIdx = leastSignificantNumericalIndex(upperTokens);
		if(lowerLsnIdx != upperLsnIdx) {
			throw new MiException("Least significant numerical indices are different");
		}
		for(int i = 0; i < lowerTokens.length; ++i) {
			if(i == lowerLsnIdx) {
				if(lowerTokens[i].toString().length() != upperTokens[i].toString().length()) {
					throw new MiException("Number of digits of least significant numerical is different");
				}
			} else if(!lowerTokens[i].equals(upperTokens[i])) {
				throw new MiException("Tokens are dirrent");
			}
		}
	}
	/**
	 * Generates a range of DNs given the first one and the number of DNs to be created. The generated
	 * DNs will be incremented with the least significant numerical part, e.g. 123#45*<b>1</b>, 123#45*<b>2</b>, 123#45*<b>3</b>, ...
	 * @param firstDn The first DN in the range. Note that DN can contain numerical plus the '#*' characters,
	 * but must contain at least one numerical character.
	 * @param numberOfDns Number of DNs to be generated
	 * @return The list of DNs generated
	 */
	public static List<String> generateDnRange(String firstDn, int numberOfDns) {
		List<String> ret = new LinkedList<String>();
		// First, break the DN into tokens
		Object[] tokens = tokenise(firstDn);
		// Then determine the least significant numerical part (this will be incremented for each subsequent DNs)
		int numIdx = leastSignificantNumericalIndex(tokens);
		// Only proceed if there is at least one numerical token in the DN
		if(numIdx > -1) {
			// Grab the incremental numerical value
			int numerical = Integer.parseInt(tokens[numIdx].toString());
			// Make sure the incremental part never changes the length of the DNs
			// I.e. making sure all DNs are of the same number of digits, e.g. 1237, 1238, 1239, but not 12310.
			int numericalStrLen = tokens[numIdx].toString().length();
			final String format = "%1$0" + numericalStrLen + 'd';
			// Generate the DNs until the whole range is created or if the incremental value exceeds the digit length
			for(int i = 0; i < numberOfDns; ++i) {
				StringBuilder dn = new StringBuilder(64);
				@SuppressWarnings("ValueOfIncrementOrDecrementUsed")
				String incrementalValue = String.format(format, numerical++);
				if(incrementalValue.length() > numericalStrLen) {
					// Exceeded the digit length, stop
					break;
				}
				for(int c = 0; c < tokens.length; ++c) {
					if(c == numIdx) {
						dn.append(incrementalValue);
					} else {
						dn.append(tokens[c]);
					}
				}
				ret.add(dn.toString());
			}
		}
		return ret;
	}
	/**
	 * Generates a range of DNs given the first one and the last DNs in the range. The generated
	 * DNs will be incremented with the least significant numerical part, e.g. 123#45*<b>1</b>, 123#45*<b>2</b>, 123#45*<b>3</b>, ...
	 * @param firstDn The first DN in the range. Note that DN can contain numerical plus the '#*' characters,
	 * but must contain at least one numerical character.
	 * @param lastDn The last DN in the range.
	 * @return The list of DNs generated
	 */
	public static List<String> generateDnRange(String firstDn, String lastDn) {
		List<String> ret = new LinkedList<String>();
		// First, break the DNs into tokens
		Object[] lowerTokens = tokenise(firstDn);
		Object[] upperTokens = tokenise(lastDn);
		try {
			validateRange(lowerTokens, upperTokens);
			int lsnIdx = leastSignificantNumericalIndex(lowerTokens);
			int lowerIncValue = Integer.parseInt(lowerTokens[lsnIdx].toString());
			int upperIncValue = Integer.parseInt(upperTokens[lsnIdx].toString());
			int numberOfDns = upperIncValue > lowerIncValue ? upperIncValue - lowerIncValue + 1 : lowerIncValue - upperIncValue + 1;
			int numericalStrLen = lowerTokens[lsnIdx].toString().length();
			final String format = "%1$0" + numericalStrLen + 'd';
			for(int i = 0; i < numberOfDns; ++i) {
				StringBuilder dn = new StringBuilder(64);
				for(int c = 0; c < lowerTokens.length; ++c) {
					if(c != lsnIdx) {
						dn.append(lowerTokens[c]);
					} else {
						dn.append(String.format(format, lowerIncValue));
					}
				}
				ret.add(dn.toString());
				if(lowerIncValue < upperIncValue) {
					++lowerIncValue;
				} else {
					--lowerIncValue;
				}
			}
		}
		catch(MiException ex) {
			MiSystem.logWarning(MiLogMsg.Category.DESIGN, "Invalid range " + firstDn + '-' + lastDn + " : " + ex);
		}
		return ret;
	}
	/**
	 * Testing
	 * @param args 
	 */
	public static void main(String[] args) {
		MiSystem.setLogLevel(2);
		String dn = "123#*#43##090";
		String dnLow = "432#*#090";
		String dnHigh = "432#*#021";
		List<String> range = generateDnRange(dn, 30);
		for(String n : range) {
			MiSystem.logInfo(MiLogMsg.Category.DESIGN, n);
		}
		range = generateDnRange(dnLow, dnHigh);
		for(String n : range) {
			MiSystem.logInfo(MiLogMsg.Category.DESIGN, n);
		}
	}
}
