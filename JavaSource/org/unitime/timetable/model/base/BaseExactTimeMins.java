/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.model.base;

import java.io.Serializable;

import org.unitime.timetable.model.ExactTimeMins;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BaseExactTimeMins implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private Integer iMinsPerMtgMin;
	private Integer iMinsPerMtgMax;
	private Integer iNrSlots;
	private Integer iBreakTime;


	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_MINS_MIN = "minsPerMtgMin";
	public static String PROP_MINS_MAX = "minsPerMtgMax";
	public static String PROP_NR_SLOTS = "nrSlots";
	public static String PROP_BREAK_TIME = "breakTime";

	public BaseExactTimeMins() {
		initialize();
	}

	public BaseExactTimeMins(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public Integer getMinsPerMtgMin() { return iMinsPerMtgMin; }
	public void setMinsPerMtgMin(Integer minsPerMtgMin) { iMinsPerMtgMin = minsPerMtgMin; }

	public Integer getMinsPerMtgMax() { return iMinsPerMtgMax; }
	public void setMinsPerMtgMax(Integer minsPerMtgMax) { iMinsPerMtgMax = minsPerMtgMax; }

	public Integer getNrSlots() { return iNrSlots; }
	public void setNrSlots(Integer nrSlots) { iNrSlots = nrSlots; }

	public Integer getBreakTime() { return iBreakTime; }
	public void setBreakTime(Integer breakTime) { iBreakTime = breakTime; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof ExactTimeMins)) return false;
		if (getUniqueId() == null || ((ExactTimeMins)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((ExactTimeMins)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "ExactTimeMins["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "ExactTimeMins[" +
			"\n	BreakTime: " + getBreakTime() +
			"\n	MinsPerMtgMax: " + getMinsPerMtgMax() +
			"\n	MinsPerMtgMin: " + getMinsPerMtgMin() +
			"\n	NrSlots: " + getNrSlots() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
