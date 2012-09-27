/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 */

package jgloss.dictionary;

/**
 * Test result for {@link DictionaryImplementation}.
 *
 * @author Michael Koch
 */
public class TestResult {
	private final float confidence;
	private final String reason;

	public TestResult(float _confidence, String _reason) {
		this.confidence = _confidence;
		this.reason = _reason;
	}

	/**
	 * Confidence of the descriptor pointing to an instance of this dictionary type.
	 * The higher the number, the greater the confidence.
	 * Returns {@link #ZERO_CONFIDENCE ZERO_CONFIDENCE} if the descriptor does not
	 * match this type.
	 */
	public float getConfidence() { return confidence; }
	/**
	 * Short description of how the confidence was calculated.
	 */
	public String getReason() { return reason; }
}