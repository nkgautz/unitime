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
package org.unitime.timetable.gwt.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.cpsolver.coursett.model.Placement;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.SecurityMessages;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.client.sectioning.SectioningStatusFilterBox.SectioningStatusFilterRpcRequest;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.services.SectioningService;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ClassAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.EnrollmentInfo;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.SectioningAction;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.CheckCoursesResponse;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourse;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourseStatus;
import org.unitime.timetable.gwt.shared.DegreePlanInterface;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck.EligibilityFlag;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.SectioningProperties;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.StudentGroupInfo;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.StudentStatusInfo;
import org.unitime.timetable.gwt.shared.PageAccessException;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.RetrieveAllSpecialRegistrationsRequest;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.RetrieveSpecialRegistrationRequest;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.RetrieveSpecialRegistrationResponse;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SpecialRegistrationEligibilityRequest;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SpecialRegistrationEligibilityResponse;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SubmitSpecialRegistrationRequest;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SubmitSpecialRegistrationResponse;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseCreditUnitConfig;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseRequest;
import org.unitime.timetable.model.CourseRequestOption;
import org.unitime.timetable.model.CourseType;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Reservation;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentAccomodation;
import org.unitime.timetable.model.StudentAreaClassificationMajor;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.StudentGroupReservation;
import org.unitime.timetable.model.StudentGroupType;
import org.unitime.timetable.model.StudentSectioningStatus;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.StudentSectioningStatus.Option;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.CourseTypeDAO;
import org.unitime.timetable.model.dao.CurriculumDAO;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.model.dao.StudentGroupDAO;
import org.unitime.timetable.model.dao.StudentSectioningStatusDAO;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.basic.CheckCourses;
import org.unitime.timetable.onlinesectioning.basic.CheckEligibility;
import org.unitime.timetable.onlinesectioning.basic.CourseRequestEligibility;
import org.unitime.timetable.onlinesectioning.basic.GetAssignment;
import org.unitime.timetable.onlinesectioning.basic.GetDegreePlans;
import org.unitime.timetable.onlinesectioning.basic.GetRequest;
import org.unitime.timetable.onlinesectioning.basic.ListClasses;
import org.unitime.timetable.onlinesectioning.basic.ListCourseOfferings;
import org.unitime.timetable.onlinesectioning.basic.ListEnrollments;
import org.unitime.timetable.onlinesectioning.custom.CourseDetailsProvider;
import org.unitime.timetable.onlinesectioning.custom.CourseMatcherProvider;
import org.unitime.timetable.onlinesectioning.custom.CustomCourseRequestsHolder;
import org.unitime.timetable.onlinesectioning.custom.CustomCourseRequestsValidationHolder;
import org.unitime.timetable.onlinesectioning.custom.CustomDegreePlansHolder;
import org.unitime.timetable.onlinesectioning.custom.CustomSpecialRegistrationHolder;
import org.unitime.timetable.onlinesectioning.custom.CustomStudentEnrollmentHolder;
import org.unitime.timetable.onlinesectioning.custom.DefaultCourseDetailsProvider;
import org.unitime.timetable.onlinesectioning.custom.ExternalTermProvider;
import org.unitime.timetable.onlinesectioning.custom.RequestStudentUpdates;
import org.unitime.timetable.onlinesectioning.match.AbstractCourseMatcher;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.server.DatabaseServer;
import org.unitime.timetable.onlinesectioning.solver.ComputeSuggestionsAction;
import org.unitime.timetable.onlinesectioning.solver.FindAssignmentAction;
import org.unitime.timetable.onlinesectioning.specreg.SpecialRegistrationEligibility;
import org.unitime.timetable.onlinesectioning.specreg.SpecialRegistrationRetrieve;
import org.unitime.timetable.onlinesectioning.specreg.SpecialRegistrationRetrieveAll;
import org.unitime.timetable.onlinesectioning.specreg.SpecialRegistrationSubmit;
import org.unitime.timetable.onlinesectioning.status.FindEnrollmentAction;
import org.unitime.timetable.onlinesectioning.status.FindEnrollmentInfoAction;
import org.unitime.timetable.onlinesectioning.status.FindStudentInfoAction;
import org.unitime.timetable.onlinesectioning.status.FindOnlineSectioningLogAction;
import org.unitime.timetable.onlinesectioning.status.StatusPageSuggestionsAction;
import org.unitime.timetable.onlinesectioning.status.db.DbFindEnrollmentAction;
import org.unitime.timetable.onlinesectioning.status.db.DbFindEnrollmentInfoAction;
import org.unitime.timetable.onlinesectioning.status.db.DbFindOnlineSectioningLogAction;
import org.unitime.timetable.onlinesectioning.status.db.DbFindStudentInfoAction;
import org.unitime.timetable.onlinesectioning.updates.ApproveEnrollmentsAction;
import org.unitime.timetable.onlinesectioning.updates.ChangeStudentGroup;
import org.unitime.timetable.onlinesectioning.updates.ChangeStudentStatus;
import org.unitime.timetable.onlinesectioning.updates.EnrollStudent;
import org.unitime.timetable.onlinesectioning.updates.MassCancelAction;
import org.unitime.timetable.onlinesectioning.updates.RejectEnrollmentsAction;
import org.unitime.timetable.onlinesectioning.updates.SaveStudentRequests;
import org.unitime.timetable.onlinesectioning.updates.StudentEmail;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.UserAuthority;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.context.AnonymousUserContext;
import org.unitime.timetable.security.permissions.AdministrationPermissions.Chameleon;
import org.unitime.timetable.security.qualifiers.SimpleQualifier;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.service.ProxyHolder;
import org.unitime.timetable.solver.service.SolverServerService;
import org.unitime.timetable.solver.service.SolverService;
import org.unitime.timetable.solver.studentsct.BatchEnrollStudent;
import org.unitime.timetable.solver.studentsct.StudentSolverProxy;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.Formats;
import org.unitime.timetable.util.LoginManager;
import org.unitime.timetable.util.NameFormat;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author Tomas Muller
 */
@Service("sectioning.gwt")
public class SectioningServlet implements SectioningService, DisposableBean {
	private static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	private static StudentSectioningConstants CONSTANTS = Localization.create(StudentSectioningConstants.class);
	private static SecurityMessages SEC_MSG = Localization.create(SecurityMessages.class);
	private static Logger sLog = Logger.getLogger(SectioningServlet.class);
	private CourseDetailsProvider iCourseDetailsProvider;
	private CourseMatcherProvider iCourseMatcherProvider;
	private ExternalTermProvider iExternalTermProvider;
	
	public SectioningServlet() {
	}
	
	private CourseDetailsProvider getCourseDetailsProvider() {
		if (iCourseDetailsProvider == null) {
			try {
				String providerClass = ApplicationProperty.CustomizationCourseDetails.value();
				if (providerClass != null)
					iCourseDetailsProvider = (CourseDetailsProvider)Class.forName(providerClass).newInstance();
			} catch (Exception e) {
				sLog.warn("Failed to initialize course detail provider: " + e.getMessage());
				iCourseDetailsProvider = new DefaultCourseDetailsProvider();
			}
		}
		return iCourseDetailsProvider;
	}
	
	private CourseMatcherProvider getCourseMatcherProvider() {
		if (iCourseMatcherProvider == null) {
			try {
				String providerClass = ApplicationProperty.CustomizationCourseMatcher.value();
				if (providerClass != null)
					iCourseMatcherProvider = (CourseMatcherProvider)Class.forName(providerClass).newInstance();
			} catch (Exception e) {
				sLog.warn("Failed to initialize course matcher provider: " + e.getMessage());
			}
		}
		return iCourseMatcherProvider;
	}
	
	private ExternalTermProvider getExternalTermProvider() {
		if (iExternalTermProvider == null) {
			try {
				String providerClass = ApplicationProperty.CustomizationExternalTerm.value();
				if (providerClass != null)
					iExternalTermProvider = (ExternalTermProvider)Class.forName(providerClass).newInstance();
			} catch (Exception e) {
				sLog.warn("Failed to initialize external term provider: " + e.getMessage());
			}
		}
		return iExternalTermProvider;
	}
	
	private @Autowired AuthenticationManager authenticationManager;
	private AuthenticationManager getAuthenticationManager() { return authenticationManager; }
	private @Autowired SessionContext sessionContext;
	private SessionContext getSessionContext() { return sessionContext; }
	private @Autowired SolverService<StudentSolverProxy> studentSectioningSolverService;
	private StudentSolverProxy getStudentSolver() { return studentSectioningSolverService.getSolver(); }
	private @Autowired SolverServerService solverServerService;
	private OnlineSectioningServer getServerInstance(Long academicSessionId, boolean canReturnDummy) {
		if (academicSessionId == null) return null;
		OnlineSectioningServer server =  solverServerService.getOnlineStudentSchedulingContainer().getSolver(academicSessionId.toString());
		if (server != null || !canReturnDummy) return server;
		
		ProxyHolder<Long, OnlineSectioningServer> h = (ProxyHolder<Long, OnlineSectioningServer>)sessionContext.getAttribute("OnlineSectioning.DummyServer");
		if (h != null && h.isValid(academicSessionId))
			return h.getProxy();
		
		Session session = SessionDAO.getInstance().get(academicSessionId);
		if (session == null)
			throw new SectioningException(MSG.exceptionBadSession()); 
		server = new DatabaseServer(new AcademicSessionInfo(session), false);
		sessionContext.setAttribute("OnlineSectioning.DummyServer", new ProxyHolder<Long, OnlineSectioningServer>(academicSessionId, server));
		
		return server;
	}

	public Collection<ClassAssignmentInterface.CourseAssignment> listCourseOfferings(Long sessionId, String query, Integer limit) throws SectioningException, PageAccessException {
		if (sessionId==null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		setLastSessionId(sessionId);
		
		CourseMatcher matcher = getCourseMatcher(sessionId);
		
		OnlineSectioningServer server = getServerInstance(sessionId, false);
		
		if (server == null) {
			String types = "";
			for (String ref: matcher.getAllowedCourseTypes())
				types += (types.isEmpty() ? "" : ", ") + "'" + ref + "'";
			if (!matcher.isAllCourseTypes() && !matcher.isNoCourseType() && types.isEmpty()) throw new SectioningException(MSG.exceptionCourseDoesNotExist(query));
			
			boolean excludeNotOffered = ApplicationProperty.CourseRequestsShowNotOffered.isFalse();
			ArrayList<ClassAssignmentInterface.CourseAssignment> results = new ArrayList<ClassAssignmentInterface.CourseAssignment>();
			org.hibernate.Session hibSession = CurriculumDAO.getInstance().getSession();
			org.unitime.timetable.onlinesectioning.match.CourseMatcher parent = matcher.getParentCourseMatcher();
			for (CourseOffering c: (List<CourseOffering>)hibSession.createQuery(
					"select c from CourseOffering c where " +
					(excludeNotOffered ? "c.instructionalOffering.notOffered is false and " : "") +
					"c.subjectArea.session.uniqueId = :sessionId and c.subjectArea.department.allowStudentScheduling = true and (" +
					"(lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr) like :q || '%' or lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr || ' - ' || c.title) like :q || '%') " +
					(query.length()>2 ? "or lower(c.title) like '%' || :q || '%'" : "") + ") " +
					(matcher.isAllCourseTypes() ? "" : matcher.isNoCourseType() ? types.isEmpty() ? " and c.courseType is null " : " and (c.courseType is null or c.courseType.reference in (" + types + ")) " : " and c.courseType.reference in (" + types + ") ") +
					"order by case " +
					"when lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr) like :q || '%' then 0 else 1 end," + // matches on course name first
					"c.subjectArea.subjectAreaAbbreviation, c.courseNbr")
					.setString("q", query.toLowerCase())
					.setLong("sessionId", sessionId)
					.setCacheable(true).setMaxResults(limit == null || limit <= 0 || parent != null ? Integer.MAX_VALUE : limit).list()) {
				if (parent != null && !parent.match(new XCourseId(c))) continue;
				CourseAssignment course = new CourseAssignment();
				course.setCourseId(c.getUniqueId());
				course.setSubject(c.getSubjectAreaAbbv());
				course.setCourseNbr(c.getCourseNbr());
				course.setTitle(c.getTitle());
				course.setNote(c.getScheduleBookNote());
				if (c.getCredit() != null) {
					course.setCreditText(c.getCredit().creditText());
					course.setCreditAbbv(c.getCredit().creditAbbv());
				}
				course.setTitle(c.getTitle());
				course.setHasUniqueName(true);
				course.setHasCrossList(c.getInstructionalOffering().hasCrossList());
				boolean unlimited = false;
				int courseLimit = 0;
				for (Iterator<InstrOfferingConfig> i = c.getInstructionalOffering().getInstrOfferingConfigs().iterator(); i.hasNext(); ) {
					InstrOfferingConfig cfg = i.next();
					if (cfg.isUnlimitedEnrollment()) unlimited = true;
					if (cfg.getLimit() != null) courseLimit += cfg.getLimit();
				}
				if (c.getReservation() != null)
					courseLimit = c.getReservation();
	            if (courseLimit >= 9999) unlimited = true;
				course.setLimit(unlimited ? -1 : courseLimit);
				course.setProjected(c.getProjectedDemand());
				course.setEnrollment(c.getEnrollment());
				course.setLastLike(c.getDemand());
				results.add(course);
				for (InstrOfferingConfig config: c.getInstructionalOffering().getInstrOfferingConfigs()) {
					if (config.getInstructionalMethod() != null)
						course.addInstructionalMethod(config.getInstructionalMethod().getUniqueId(), config.getInstructionalMethod().getLabel());
					else
						course.setHasNoInstructionalMethod(true);
				}
				if (parent != null && limit != null && limit > 0 && results.size() >= limit) break;
			}
			if (results.isEmpty()) {
				throw new SectioningException(MSG.exceptionCourseDoesNotExist(query));
			}
			return results;
		} else {
			Collection<ClassAssignmentInterface.CourseAssignment> results = null;
			try {
				results = server.execute(server.createAction(ListCourseOfferings.class).forQuery(query).withLimit(limit).withMatcher(matcher), currentUser());
			} catch (PageAccessException e) {
				throw e;
			} catch (SectioningException e) {
				throw e;
			} catch (Exception e) {
				sLog.error(e.getMessage(), e);
				throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
			}
			if (results == null || results.isEmpty()) {
				throw new SectioningException(MSG.exceptionCourseDoesNotExist(query));
			}
			return results;
		}
	}
	
	public CourseMatcher getCourseMatcher(Long sessionId) {
		boolean noCourseType = true, allCourseTypes = false;
		Set<String> allowedCourseTypes = new HashSet<String>();
		Long studentId = getStudentId(sessionId);
		if (getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor)) {
			allCourseTypes = true;
		} else {
			org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
			try {
				Student student = (studentId == null ? null : StudentDAO.getInstance().get(studentId, hibSession));
				StudentSectioningStatus status = (student == null ? null : student.getEffectiveStatus());
				if (status != null) {
					for (CourseType type: status.getTypes())
						allowedCourseTypes.add(type.getReference());
					noCourseType = !status.hasOption(Option.notype);
				}
			} finally {
				hibSession.close();
			}
		}
		CourseMatcher matcher = new CourseMatcher(allCourseTypes, noCourseType, allowedCourseTypes);
		
		if (studentId != null) {
			CourseMatcherProvider provider = getCourseMatcherProvider();
			if (provider != null) matcher.setParentCourseMatcher(provider.getCourseMatcher(getSessionContext(), studentId));
		}
		
		return matcher;
	}
	
	@SuppressWarnings("unchecked")
	public Collection<ClassAssignmentInterface.ClassAssignment> listClasses(boolean online, Long sessionId, String course) throws SectioningException, PageAccessException {
		if (sessionId==null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		if (!online) {
			OnlineSectioningServer server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());
			else
				return server.execute(server.createAction(ListClasses.class).forCourseAndStudent(course, getStudentId(sessionId)), currentUser());
		}
		setLastSessionId(sessionId);
		Long studentId = getStudentId(sessionId);
		OnlineSectioningServer server = getServerInstance(sessionId, false);
		Set<Long> allowedClasses = null;
		if (server == null) {
			ArrayList<ClassAssignmentInterface.ClassAssignment> results = new ArrayList<ClassAssignmentInterface.ClassAssignment>();
			org.hibernate.Session hibSession = CurriculumDAO.getInstance().getSession();
			CourseOffering courseOffering = null;
			for (CourseOffering c: (List<CourseOffering>)hibSession.createQuery(
					"select c from CourseOffering c where " +
					"c.subjectArea.session.uniqueId = :sessionId and c.subjectArea.department.allowStudentScheduling = true and " +
					"(lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr) = :course or lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr || ' - ' || c.title) = :course)")
					.setString("course", course.toLowerCase())
					.setLong("sessionId", sessionId)
					.setCacheable(true).setMaxResults(1).list()) {
				courseOffering = c; break;
			}
			if (courseOffering == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(course));
			List<Class_> classes = new ArrayList<Class_>();
			for (Iterator<InstrOfferingConfig> i = courseOffering.getInstructionalOffering().getInstrOfferingConfigs().iterator(); i.hasNext(); ) {
				InstrOfferingConfig config = i.next();
				for (Iterator<SchedulingSubpart> j = config.getSchedulingSubparts().iterator(); j.hasNext(); ) {
					SchedulingSubpart subpart = j.next();
					classes.addAll(subpart.getClasses());
				}
			}
			Collections.sort(classes, new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
			NameFormat nameFormat = NameFormat.fromReference(ApplicationProperty.OnlineSchedulingInstructorNameFormat.value());
			for (Class_ clazz: classes) {
				if (!clazz.isEnabledForStudentScheduling()) {
					if (studentId != null && allowedClasses == null) {
						allowedClasses = new HashSet<Long>();
						for (Reservation reservation: courseOffering.getInstructionalOffering().getReservations()) {
							if (reservation instanceof StudentGroupReservation) {
								StudentGroupType type = ((StudentGroupReservation)reservation).getGroup().getType();
								if (type != null && type.getAllowDisabledSection() == StudentGroupType.AllowDisabledSection.WithGroupReservation) {
									boolean hasStudent = false;
									for (Student student: ((StudentGroupReservation)reservation).getGroup().getStudents()) {
										if (student.getUniqueId().equals(studentId)) {
											hasStudent = true; break;
										}
									}
									if (hasStudent) {
										for (Class_ c: classes)
											if (!c.isEnabledForStudentScheduling() && reservation.isMatching(c))
												allowedClasses.add(c.getUniqueId());
									}
								}
							}
						}
						Student student = StudentDAO.getInstance().get(studentId, hibSession);
						if (student != null) {
							for (StudentGroup group: student.getGroups()) {
								StudentGroupType type = group.getType();
								if (type != null && type.getAllowDisabledSection() == StudentGroupType.AllowDisabledSection.AlwaysAllowed) {
									for (Class_ c: classes)
										if (!c.isEnabledForStudentScheduling())
											allowedClasses.add(c.getUniqueId());
									break;
								}
							}
						}
					}
					if (allowedClasses == null || !allowedClasses.contains(clazz.getUniqueId())) continue;
				}
				ClassAssignmentInterface.ClassAssignment a = new ClassAssignmentInterface.ClassAssignment();
				a.setClassId(clazz.getUniqueId());
				a.setSubpart(clazz.getSchedulingSubpart().getItypeDesc());
				if (clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalMethod() != null)
					a.setSubpart(clazz.getSchedulingSubpart().getItypeDesc() + " (" + clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalMethod().getLabel() + ")");
				a.setSection(clazz.getClassSuffix(courseOffering));
				a.setClassNumber(clazz.getSectionNumberString(hibSession));
				a.addNote(clazz.getSchedulePrintNote());

				Assignment ass = clazz.getCommittedAssignment();
				Placement p = (ass == null ? null : ass.getPlacement());
				
                int minLimit = clazz.getExpectedCapacity();
            	int maxLimit = clazz.getMaxExpectedCapacity();
            	int limit = maxLimit;
            	if (minLimit < maxLimit && p != null) {
            		// int roomLimit = Math.round((clazz.getRoomRatio() == null ? 1.0f : clazz.getRoomRatio()) * p.getRoomSize());
            		int roomLimit = (int) Math.floor(p.getRoomSize() / (clazz.getRoomRatio() == null ? 1.0f : clazz.getRoomRatio()));
            		limit = Math.min(Math.max(minLimit, roomLimit), maxLimit);
            	}
                if (clazz.getSchedulingSubpart().getInstrOfferingConfig().isUnlimitedEnrollment() || limit >= 9999) limit = -1;
                a.setCancelled(clazz.isCancelled());
				a.setLimit(new int[] {clazz.getEnrollment() == 0 ? -1 : clazz.getEnrollment(), limit});
				
				if (p != null && p.getTimeLocation() != null) {
					for (DayCode d: DayCode.toDayCodes(p.getTimeLocation().getDayCode()))
						a.addDay(d.getIndex());
					a.setStart(p.getTimeLocation().getStartSlot());
					a.setLength(p.getTimeLocation().getLength());
					a.setBreakTime(p.getTimeLocation().getBreakTime());
					a.setDatePattern(p.getTimeLocation().getDatePatternName());
				}
				if (ass != null)
					for (Location loc: ass.getRooms())
						a.addRoom(loc.getUniqueId(), loc.getLabelWithDisplayName());
				/*
				if (p != null && p.getRoomLocations() != null) {
					for (RoomLocation rm: p.getRoomLocations()) {
						a.addRoom(rm.getId(), rm.getName());
					}
				}
				if (p != null && p.getRoomLocation() != null) {
					a.addRoom(p.getRoomLocation().getId(), p.getRoomLocation().getName());
				}
				*/
				if (!clazz.getClassInstructors().isEmpty()) {
					for (Iterator<ClassInstructor> i = clazz.getClassInstructors().iterator(); i.hasNext(); ) {
						ClassInstructor instr = i.next();
						a.addInstructor(nameFormat.format(instr.getInstructor()));
						a.addInstructoEmail(instr.getInstructor().getEmail());
					}
				}
				if (clazz.getParentClass() != null)
					a.setParentSection(clazz.getParentClass().getClassSuffix(courseOffering));
				a.setSubpartId(clazz.getSchedulingSubpart().getUniqueId());
				if (a.getParentSection() == null)
					a.setParentSection(courseOffering.getConsentType() == null ? null : courseOffering.getConsentType().getLabel());
				results.add(a);
			}
			return results;
		} else {
			try {
				return server.execute(server.createAction(ListClasses.class).forCourseAndStudent(course, getStudentId(sessionId)), currentUser());
			} catch (PageAccessException e) {
				throw e;
			} catch (SectioningException e) {
				throw e;
			} catch (Exception e) {
				sLog.error(e.getMessage(), e);
				throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
			}
		}
	}

	public Collection<AcademicSessionProvider.AcademicSessionInfo> listAcademicSessions(boolean sectioning) throws SectioningException, PageAccessException {
		ArrayList<AcademicSessionProvider.AcademicSessionInfo> ret = new ArrayList<AcademicSessionProvider.AcademicSessionInfo>();
		ExternalTermProvider extTerm = getExternalTermProvider();
		UniTimePrincipal principal = (UniTimePrincipal)getSessionContext().getAttribute("user");
		if (sectioning) {
			for (String s: solverServerService.getOnlineStudentSchedulingContainer().getSolvers()) {
				OnlineSectioningServer server = solverServerService.getOnlineStudentSchedulingContainer().getSolver(s);
				if (server == null || !server.isReady()) continue;
				Session session = SessionDAO.getInstance().get(Long.valueOf(s));
				AcademicSessionInfo info = server.getAcademicSession();
				if (principal != null) {
					Long studentId = principal.getStudentId(session.getUniqueId());
					if (studentId == null) continue;
					Student student = StudentDAO.getInstance().get(studentId);
					if (student == null) continue;
					StudentSectioningStatus status = student.getEffectiveStatus();
					if (status != null && !status.hasOption(StudentSectioningStatus.Option.enabled)) continue;
				} else {
					if (!getSessionContext().hasPermissionAnySession(session, Right.SchedulingAssistant)) continue;
				}
				ret.add(new AcademicSessionProvider.AcademicSessionInfo(
						session.getUniqueId(),
						session.getAcademicYear(), session.getAcademicTerm(), session.getAcademicInitiative(),
						MSG.sessionName(session.getAcademicYear(), session.getAcademicTerm(), session.getAcademicInitiative()))
						.setExternalCampus(extTerm == null ? null : extTerm.getExternalCampus(info))
						.setExternalTerm(extTerm == null ? null : extTerm.getExternalTerm(info)));
			}
		} else {
			for (Session session: SessionDAO.getInstance().findAll()) {
				if (session.getStatusType().isTestSession()) continue;
				if (session.getStatusType().canPreRegisterStudents()) {
					AcademicSessionInfo info = new AcademicSessionInfo(session);
					if (principal != null) {
						Long studentId = principal.getStudentId(session.getUniqueId());
						if (studentId == null) continue;
						Student student = StudentDAO.getInstance().get(studentId);
						if (student == null) continue;
						StudentSectioningStatus status = student.getEffectiveStatus();
						if (status != null && !status.hasOption(StudentSectioningStatus.Option.regenabled)) continue;
					} else {
						if (!getSessionContext().hasPermissionAnySession(session, Right.CourseRequests)) continue;
					}
					ret.add(new AcademicSessionProvider.AcademicSessionInfo(
							session.getUniqueId(),
							session.getAcademicYear(), session.getAcademicTerm(), session.getAcademicInitiative(),
							MSG.sessionName(session.getAcademicYear(), session.getAcademicTerm(), session.getAcademicInitiative()))
							.setExternalCampus(extTerm == null ? null : extTerm.getExternalCampus(info))
							.setExternalTerm(extTerm == null ? null : extTerm.getExternalTerm(info)));
				}
			}
		}
		if (ret.isEmpty()) {
			throw new SectioningException(MSG.exceptionNoSuitableAcademicSessions());
		}
		return ret;
	}
	
	public String retrieveCourseDetails(Long sessionId, String course) throws SectioningException, PageAccessException {
		setLastSessionId(sessionId);
		OnlineSectioningServer server = getServerInstance(sessionId, false); 
		if (server == null) {
			CourseOffering courseOffering = lookupCourse(CourseOfferingDAO.getInstance().getSession(), sessionId, null, course, null);
			if (courseOffering == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(course));
			return getCourseDetailsProvider().getDetails(
					new AcademicSessionInfo(courseOffering.getSubjectArea().getSession()),
					courseOffering.getSubjectAreaAbbv(), courseOffering.getCourseNbr());
		} else {
			XCourseId c = server.getCourse(course);
			if (c == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(course));
			return server.getCourseDetails(c.getCourseId(), getCourseDetailsProvider());
		}
	}
	
	public Long retrieveCourseOfferingId(Long sessionId, String course) throws SectioningException, PageAccessException {
		setLastSessionId(sessionId);
		OnlineSectioningServer server = getServerInstance(sessionId, false); 
		if (server == null) {
			CourseOffering courseOffering = lookupCourse(CourseOfferingDAO.getInstance().getSession(), sessionId, null, course, null);
			if (courseOffering == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(course));
			return courseOffering.getUniqueId();
		} else {
			XCourseId c = server.getCourse(course);
			if (c == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(course));
			return c.getCourseId();
		}
	}

	public ClassAssignmentInterface section(boolean online, CourseRequestInterface request, ArrayList<ClassAssignmentInterface.ClassAssignment> currentAssignment) throws SectioningException, PageAccessException {
		try {
			if (!online) {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());
				request.setStudentId(getStudentId(request.getAcademicSessionId()));
				ClassAssignmentInterface ret = server.execute(server.createAction(FindAssignmentAction.class).forRequest(request).withAssignment(currentAssignment), currentUser()).get(0);
				if (ret != null)
					ret.setCanEnroll(getStudentId(request.getAcademicSessionId()) != null);
				return ret;
			}
			
			setLastSessionId(request.getAcademicSessionId());
			setLastRequest(request);
			request.setStudentId(getStudentId(request.getAcademicSessionId()));
			OnlineSectioningServer server = getServerInstance(request.getAcademicSessionId(), true);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			ClassAssignmentInterface ret = server.execute(server.createAction(FindAssignmentAction.class).forRequest(request).withAssignment(currentAssignment), currentUser()).get(0);
			if (ret != null) {
				ret.setCanEnroll(server.getAcademicSession().isSectioningEnabled());
				if (ret.isCanEnroll()) {
					if (getStudentId(request.getAcademicSessionId()) == null)
						ret.setCanEnroll(false);
				}
			}
			return ret;
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionSectioningFailed(e.getMessage()), e);
		}
	}
	
	public CheckCoursesResponse checkCourses(boolean online, boolean sectioning, CourseRequestInterface request) throws SectioningException, PageAccessException {
		try {
			if (request.getAcademicSessionId() == null) throw new SectioningException(MSG.exceptionNoAcademicSession());
			if (request.getStudentId() == null)
				request.setStudentId(getStudentId(request.getAcademicSessionId()));
			
			if (!online) {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());
				return server.execute(server.createAction(CheckCourses.class).forRequest(request), currentUser());
			}
			
			setLastSessionId(request.getAcademicSessionId());
			setLastRequest(request);
			OnlineSectioningServer server = getServerInstance(request.getAcademicSessionId(), false);
			if (server == null) {
				if (!sectioning && CustomCourseRequestsValidationHolder.hasProvider()) {
					OnlineSectioningServer dummy = getServerInstance(request.getAcademicSessionId(), true);
					return dummy.execute(dummy.createAction(CheckCourses.class).forRequest(request).withMatcher(getCourseMatcher(request.getAcademicSessionId())).withCustomValidation(true), currentUser());
				}
				org.hibernate.Session hibSession = CurriculumDAO.getInstance().getSession();
				CheckCoursesResponse response = new CheckCoursesResponse();
				CourseMatcher matcher = getCourseMatcher(request.getAcademicSessionId());
				Long studentId = getStudentId(request.getAcademicSessionId());
				for (CourseRequestInterface.Request cr: request.getCourses()) {
					if (cr.hasRequestedCourse()) {
						for (RequestedCourse rc: cr.getRequestedCourse())
							if (rc.isCourse() && lookupCourse(hibSession, request.getAcademicSessionId(), studentId, rc, matcher) == null) {
								response.addError(rc.getCourseId(), rc.getCourseName(), "NOT_FOUND", MSG.validationCourseNotExists(rc.getCourseName()));
								response.setErrorMessage(MSG.validationCourseNotExists(rc.getCourseName()));
							}
					}
				}
				for (CourseRequestInterface.Request cr: request.getAlternatives()) {
					if (cr.hasRequestedCourse()) {
						for (RequestedCourse rc: cr.getRequestedCourse())
							if (rc.isCourse() && lookupCourse(hibSession, request.getAcademicSessionId(), studentId, rc, matcher) == null) {
								response.addError(rc.getCourseId(), rc.getCourseName(), "NOT_FOUND", MSG.validationCourseNotExists(rc.getCourseName()));
								response.setErrorMessage(MSG.validationCourseNotExists(rc.getCourseName()));
							}
					}
				}
				return response;
			} else {
				return server.execute(server.createAction(CheckCourses.class).forRequest(request).withMatcher(getCourseMatcher(request.getAcademicSessionId())).withCustomValidation(!sectioning), currentUser());
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionSectioningFailed(e.getMessage()), e);
		}
	}
	
	public static CourseOffering lookupCourse(org.hibernate.Session hibSession, Long sessionId, Long studentId, String courseName, CourseMatcher courseMatcher) {
		if (studentId != null) {
			for (CourseOffering co: (List<CourseOffering>)hibSession.createQuery(
					"select cr.courseOffering from CourseRequest cr where " +
					"cr.courseDemand.student.uniqueId = :studentId and " +
					"(lower(cr.courseOffering.subjectArea.subjectAreaAbbreviation || ' ' || cr.courseOffering.courseNbr) = :course or " +
					"lower(cr.courseOffering.subjectArea.subjectAreaAbbreviation || ' ' || cr.courseOffering.courseNbr || ' - ' || cr.courseOffering.title) = :course)")
					.setString("course", courseName.toLowerCase())
					.setLong("studentId", studentId)
					.setCacheable(true).setMaxResults(1).list()) {
				return co;
			}
		}
		for (CourseOffering co: (List<CourseOffering>)hibSession.createQuery(
				"select c from CourseOffering c where " +
				"c.subjectArea.session.uniqueId = :sessionId and c.subjectArea.department.allowStudentScheduling = true and " +
				"(lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr) = :course or lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr || ' - ' || c.title) = :course)")
				.setString("course", courseName.toLowerCase())
				.setLong("sessionId", sessionId)
				.setCacheable(true).setMaxResults(1).list()) {
			if (courseMatcher != null && !courseMatcher.match(new XCourse(co))) continue;
			return co;
		}
		return null;
	}
	
	public static CourseOffering lookupCourse(org.hibernate.Session hibSession, Long sessionId, Long studentId, RequestedCourse rc, CourseMatcher courseMatcher) {
		if (rc.hasCourseId()) {
			CourseOffering co = CourseOfferingDAO.getInstance().get(rc.getCourseId(), hibSession);
			if (courseMatcher != null && !courseMatcher.match(new XCourse(co))) return null;
			return co;
		}
		if (rc.hasCourseName())
			return lookupCourse(hibSession, sessionId, studentId, rc.getCourseName(), courseMatcher);
		return null;
	}
	
	public 	Collection<ClassAssignmentInterface> computeSuggestions(boolean online, CourseRequestInterface request, Collection<ClassAssignmentInterface.ClassAssignment> currentAssignment, int selectedAssignmentIndex, String filter) throws SectioningException, PageAccessException {
		try {
			if (!online) {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());
				
				request.setStudentId(getStudentId(request.getAcademicSessionId()));
				ClassAssignmentInterface.ClassAssignment selectedAssignment = null;
				if (selectedAssignmentIndex >= 0) {
					selectedAssignment = ((List<ClassAssignmentInterface.ClassAssignment>)currentAssignment).get(selectedAssignmentIndex);
				} else if (request.getLastCourse() != null) {
					XCourseId course = server.getCourse(request.getLastCourse().getCourseId(), request.getLastCourse().getCourseName());
					if (course == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(request.getLastCourse().getCourseName()));
					selectedAssignment = new ClassAssignmentInterface.ClassAssignment();
					selectedAssignment.setCourseId(course.getCourseId());
				}
				
				Collection<ClassAssignmentInterface> ret = server.execute(server.createAction(ComputeSuggestionsAction.class).forRequest(request).withAssignment(currentAssignment).withSelection(selectedAssignment).withFilter(filter), currentUser());
				if (ret != null) {
					boolean canEnroll = (getStudentId(request.getAcademicSessionId()) != null);
					for (ClassAssignmentInterface ca: ret)
						ca.setCanEnroll(canEnroll);
				}
				return ret;
			}
			
			setLastSessionId(request.getAcademicSessionId());
			if (selectedAssignmentIndex >= 0)
				setLastRequest(request);
			request.setStudentId(getStudentId(request.getAcademicSessionId()));
			OnlineSectioningServer server = getServerInstance(request.getAcademicSessionId(), true);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			ClassAssignmentInterface.ClassAssignment selectedAssignment = null;
			if (selectedAssignmentIndex >= 0) {
				selectedAssignment = ((List<ClassAssignmentInterface.ClassAssignment>)currentAssignment).get(selectedAssignmentIndex);
			} else if (request.getLastCourse() != null) {
				XCourseId course = server.getCourse(request.getLastCourse().getCourseId(), request.getLastCourse().getCourseName());
				if (course == null) throw new SectioningException(MSG.exceptionCourseDoesNotExist(request.getLastCourse().getCourseName()));
				selectedAssignment = new ClassAssignmentInterface.ClassAssignment();
				selectedAssignment.setCourseId(course.getCourseId());
			}
			Collection<ClassAssignmentInterface> ret = server.execute(server.createAction(ComputeSuggestionsAction.class).forRequest(request).withAssignment(currentAssignment).withSelection(selectedAssignment).withFilter(filter), currentUser());
			if (ret != null) {
				boolean canEnroll = server.getAcademicSession().isSectioningEnabled();
				if (canEnroll) {
					if (getStudentId(request.getAcademicSessionId()) == null)
						canEnroll = false;
				}
				for (ClassAssignmentInterface ca: ret)
					ca.setCanEnroll(canEnroll);
			}
			return ret;
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionSectioningFailed(e.getMessage()), e);
		}
	}
	
	public String logIn(String userName, String password, String pin) throws SectioningException, PageAccessException {
		if (pin != null && !pin.isEmpty())
			getSessionContext().setAttribute("pin", pin);
		else
			getSessionContext().removeAttribute("pin");
		if ("LOOKUP".equals(userName)) {
			getSessionContext().checkPermissionAnySession(Right.StudentSchedulingAdvisor);
			org.hibernate.Session hibSession = StudentDAO.getInstance().createNewSession();
			try {
				List<Student> student = hibSession.createQuery("select m from Student m where m.externalUniqueId = :uid").setString("uid", password).list();
				if (!student.isEmpty()) {
					UserContext user = getSessionContext().getUser();
					UniTimePrincipal principal = new UniTimePrincipal(user.getTrueExternalUserId(), password, user.getTrueName());
					for (Student s: student) {
						if (getSessionContext().hasPermissionAnySession(s.getSession(), Right.StudentSchedulingAdvisor)) {
							principal.addStudentId(s.getSession().getUniqueId(), s.getUniqueId());
							principal.setName(NameFormat.defaultFormat().format(s));
						}
					}
					getSessionContext().setAttribute("user", principal);
					getSessionContext().removeAttribute("request");
					return principal.getName();
				}
			} finally {
				hibSession.close();
			}			
		}
		if ("BATCH".equals(userName)) {
			getSessionContext().checkPermission(Right.StudentSectioningSolver);
			OnlineSectioningServer server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());
			org.hibernate.Session hibSession = StudentDAO.getInstance().createNewSession();
			try {
				Student student = StudentDAO.getInstance().get(Long.valueOf(password), hibSession);
				if (student == null)
					throw new SectioningException(MSG.exceptionLoginFailed());
				UserContext user = getSessionContext().getUser();
				UniTimePrincipal principal = new UniTimePrincipal(user.getTrueExternalUserId(), student.getExternalUniqueId(), user.getTrueName());
				principal.addStudentId(student.getSession().getUniqueId(), student.getUniqueId());
				principal.setName(NameFormat.defaultFormat().format(student));
				getSessionContext().setAttribute("user", principal);
				getSessionContext().removeAttribute("request");
				return principal.getName();
			} finally {
				hibSession.close();
			}		
		}
		try {
    		Authentication authRequest = new UsernamePasswordAuthenticationToken(userName, password);
    		Authentication authResult = getAuthenticationManager().authenticate(authRequest);
    		SecurityContextHolder.getContext().setAuthentication(authResult);
    		UserContext user = (UserContext)authResult.getPrincipal();
    		if (user.getCurrentAuthority() == null)
    			for (UserAuthority auth: user.getAuthorities(Roles.ROLE_STUDENT)) {
    				if (getLastSessionId() == null || auth.getAcademicSession().getQualifierId().equals(getLastSessionId())) {
    					user.setCurrentAuthority(auth); break;
    				}
    			}
    		LoginManager.loginSuceeded(authResult.getName());
    		return (user.getName() == null ? user.getUsername() : user.getName());
    	} catch (Exception e) {
    		LoginManager.addFailedLoginAttempt(userName, new Date());
    		throw new PageAccessException(e.getMessage(), e);
    	}
	}
	
	public Boolean logOut() throws SectioningException, PageAccessException {
		getSessionContext().removeAttribute("user");
		getSessionContext().removeAttribute("pin");
		getSessionContext().removeAttribute("sessionId");
		getSessionContext().removeAttribute("request");
		getSessionContext().removeAttribute("eligibility");
		getSessionContext().removeAttribute("specreq");
		if (getSessionContext().hasPermission(Right.StudentSchedulingAdvisor)) 
			return false;
		SecurityContextHolder.getContext().setAuthentication(null);
		return true;
	}
	
	public String whoAmI() throws SectioningException, PageAccessException {
		UniTimePrincipal principal = (UniTimePrincipal)getSessionContext().getAttribute("user");
		if (principal != null) return principal.getName();
		UserContext user = getSessionContext().getUser();
		if (user == null || user instanceof AnonymousUserContext) return null;
		return (user.getName() == null ? user.getUsername() : user.getName());
	}
	
	public Long getStudentId(Long sessionId) {
		UniTimePrincipal principal = (UniTimePrincipal)getSessionContext().getAttribute("user");
		if (principal != null)
			return principal.getStudentId(sessionId);
		UserContext user = getSessionContext().getUser();
		if (user == null) return null;
		for (UserAuthority a: user.getAuthorities(Roles.ROLE_STUDENT, new SimpleQualifier("Session", sessionId)))
			return a.getUniqueId();
		return null;
	}
	
	public Long getLastSessionId() {
		Long lastSessionId = (Long)getSessionContext().getAttribute("sessionId");
		if (lastSessionId == null) {
			UserContext user = getSessionContext().getUser();
			if (user != null) {
				Long sessionId = user.getCurrentAcademicSessionId();
				if (sessionId != null)
					lastSessionId = sessionId;
			}
		}
		return lastSessionId;
	}

	public void setLastSessionId(Long sessionId) {
		getSessionContext().setAttribute("sessionId", sessionId);
	}
	
	public CourseRequestInterface getLastRequest() {
		return (CourseRequestInterface)getSessionContext().getAttribute("request");
	}
	
	public void setLastRequest(CourseRequestInterface request) {
		if (request == null || request.getAcademicSessionId() == null)
			getSessionContext().removeAttribute("request");
		else if (request.isUpdateLastRequest())
			getSessionContext().setAttribute("request", request);
	}
	
	public AcademicSessionProvider.AcademicSessionInfo lastAcademicSession(boolean sectioning) throws SectioningException, PageAccessException {
		if (getSessionContext().isHttpSessionNew()) throw new PageAccessException(MSG.exceptionUserNotLoggedIn());
		Long sessionId = getLastSessionId();
		if (sessionId == null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		ExternalTermProvider extTerm = getExternalTermProvider();
		if (sectioning) {
			OnlineSectioningServer server = getServerInstance(sessionId, false);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			AcademicSessionInfo s = server.getAcademicSession();
			if (s == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			return new AcademicSessionProvider.AcademicSessionInfo(
					s.getUniqueId(),
					s.getYear(), s.getTerm(), s.getCampus(),
					MSG.sessionName(s.getYear(), s.getTerm(), s.getCampus()))
					.setExternalCampus(extTerm == null ? null : extTerm.getExternalCampus(s))
					.setExternalTerm(extTerm == null ? null : extTerm.getExternalTerm(s));
		} else {
			Session session = SessionDAO.getInstance().get(sessionId);
			if (session == null || session.getStatusType().isTestSession())
				throw new SectioningException(MSG.exceptionNoSuitableAcademicSessions());
			if (!session.getStatusType().canPreRegisterStudents() || session.getStatusType().canSectionAssistStudents() || session.getStatusType().canOnlineSectionStudents())
				throw new SectioningException(MSG.exceptionNoServerForSession());
			AcademicSessionInfo info = new AcademicSessionInfo(session);
			return new AcademicSessionProvider.AcademicSessionInfo(
					session.getUniqueId(),
					session.getAcademicYear(), session.getAcademicTerm(), session.getAcademicInitiative(),
					MSG.sessionName(session.getAcademicYear(), session.getAcademicTerm(), session.getAcademicInitiative()))
					.setExternalCampus(extTerm == null ? null : extTerm.getExternalCampus(info))
					.setExternalTerm(extTerm == null ? null : extTerm.getExternalTerm(info));
		}
	}
	
	public CourseRequestInterface lastRequest(boolean online, boolean sectioning, Long sessionId) throws SectioningException, PageAccessException {
		CourseRequestInterface request = getLastRequest();
		if (request != null && !request.getAcademicSessionId().equals(sessionId)) request = null;
		if (request != null && request.getCourses().isEmpty() && request.getAlternatives().isEmpty()) request = null;
		if (request != null && request.getStudentId() != null && !request.getStudentId().equals(getStudentId(sessionId))) request = null;
		if (request == null) {
			Long studentId = getStudentId(sessionId);
			request = savedRequest(online, sectioning, sessionId, studentId);
			if (request == null && studentId == null) throw new SectioningException(MSG.exceptionNoStudent());
		}
		if (request == null)
			throw new SectioningException(MSG.exceptionBadStudentId());
		if (!request.getAcademicSessionId().equals(sessionId)) throw new SectioningException(MSG.exceptionBadSession());
		if (request.getCourses().isEmpty() && request.getAlternatives().isEmpty())
			throw new SectioningException(MSG.exceptionNoRequests());
		return request;
	}
	
	public ClassAssignmentInterface lastResult(boolean online, Long sessionId) throws SectioningException, PageAccessException {
		Long studentId = getStudentId(sessionId);
		if (studentId == null) throw new SectioningException(MSG.exceptionNoStudent());
		
		if (!online) {
			OnlineSectioningServer server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());

			ClassAssignmentInterface ret = server.execute(server.createAction(GetAssignment.class).forStudent(studentId), currentUser());
			if (ret != null)
				ret.setCanEnroll(getStudentId(sessionId) != null);
			return ret;
		}
		
		try {
			OnlineSectioningServer server = getServerInstance(sessionId, false);
			if (server == null) throw new SectioningException(MSG.exceptionBadSession());
			ClassAssignmentInterface ret = server.execute(server.createAction(GetAssignment.class).forStudent(studentId), currentUser());
			if (ret == null) throw new SectioningException(MSG.exceptionBadStudentId());
			ret.setCanEnroll(server.getAcademicSession().isSectioningEnabled());
			if (ret.isCanEnroll()) {
				if (getStudentId(sessionId) == null)
					ret.setCanEnroll(false);
			}
			if (!ret.getCourseAssignments().isEmpty()) return ret;
			throw new SectioningException(MSG.exceptionNoSchedule());
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	public CourseRequestInterface saveRequest(CourseRequestInterface request) throws SectioningException, PageAccessException {
		OnlineSectioningServer server = getServerInstance(request.getAcademicSessionId(), false);
		Long studentId = getStudentId(request.getAcademicSessionId());
		if (studentId == null && getSessionContext().hasPermissionAnySession(request.getAcademicSessionId(), Right.StudentSchedulingAdvisor))
			studentId = request.getStudentId();
		if (server != null) {
			if (studentId == null)
				throw new SectioningException(MSG.exceptionEnrollNotStudent(server.getAcademicSession().toString()));
			return server.execute(server.createAction(SaveStudentRequests.class).forStudent(studentId).withRequest(request).withCustomValidation(true), currentUser());
		} else {
			if (CustomCourseRequestsValidationHolder.hasProvider()) {
				OnlineSectioningServer dummy = getServerInstance(request.getAcademicSessionId(), true);
				return dummy.execute(dummy.createAction(SaveStudentRequests.class).forStudent(studentId).withRequest(request).withCustomValidation(true), currentUser());
			}
			if (studentId == null)
				throw new SectioningException(MSG.exceptionEnrollNotStudent(SessionDAO.getInstance().get(request.getAcademicSessionId()).getLabel()));
			org.hibernate.Session hibSession = StudentDAO.getInstance().getSession();
			try {
				Student student = StudentDAO.getInstance().get(studentId, hibSession);
				if (student == null) throw new SectioningException(MSG.exceptionBadStudentId());
				SaveStudentRequests.saveRequest(null, new OnlineSectioningHelper(hibSession, currentUser()), student, request, true);
				hibSession.save(student);
				hibSession.flush();
				return request;
			} catch (PageAccessException e) {
				throw e;
			} catch (SectioningException e) {
				throw e;
			} catch (Exception e) {
				sLog.error(e.getMessage(), e);
				throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
			} finally {
				hibSession.close();
			}
		}
	}
	
	public ClassAssignmentInterface enroll(boolean online, CourseRequestInterface request, ArrayList<ClassAssignmentInterface.ClassAssignment> currentAssignment) throws SectioningException, PageAccessException {
		if (request.getStudentId() == null) {
			Long sessionId = request.getAcademicSessionId();
			if (sessionId == null) sessionId = getLastSessionId();
			if (sessionId != null) request.setStudentId(getStudentId(sessionId));
		}
		
		Long sessionId = canEnroll(online, request.getAcademicSessionId(), request.getStudentId());
		if (!request.getAcademicSessionId().equals(sessionId))
			throw new SectioningException(MSG.exceptionBadSession());
		
		if (!online) {
			OnlineSectioningServer server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());

			return server.execute(server.createAction(BatchEnrollStudent.class).forStudent(request.getStudentId()).withRequest(request).withAssignment(currentAssignment), currentUser());
		}
		
		OnlineSectioningServer server = getServerInstance(request.getAcademicSessionId(), false);
		if (server == null) throw new SectioningException(MSG.exceptionBadStudentId());
		if (!server.getAcademicSession().isSectioningEnabled())
			throw new SectioningException(MSG.exceptionNotSupportedFeature());
		
		setLastSessionId(request.getAcademicSessionId());
		setLastRequest(request);

		return server.execute(server.createAction(EnrollStudent.class).forStudent(request.getStudentId()).withRequest(request).withAssignment(currentAssignment), currentUser());
	}

	public List<Long> canApprove(Long classOrOfferingId) throws SectioningException, PageAccessException {
		try {
			UserContext user = getSessionContext().getUser();
			if (user == null) throw new PageAccessException(
					getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());
			
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			
			InstructionalOffering offering = (classOrOfferingId >= 0 ? InstructionalOfferingDAO.getInstance().get(classOrOfferingId, hibSession) : null);
			if (offering == null) {
				Class_ clazz = (classOrOfferingId < 0 ? Class_DAO.getInstance().get(-classOrOfferingId, hibSession) : null);
				if (clazz == null)
					throw new SectioningException(MSG.exceptionBadClassOrOffering());
				offering = clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering();
			}
			
			OnlineSectioningServer server = getServerInstance(offering.getControllingCourseOffering().getSubjectArea().getSessionId(), false);
			
			if (server == null) return null; //?? !server.getAcademicSession().isSectioningEnabled()
			
			List<Long> coursesToApprove = new ArrayList<Long>();
			for (CourseOffering course: offering.getCourseOfferings()) {
				if (getSessionContext().hasPermissionAnyAuthority(course, Right.ConsentApproval))
					coursesToApprove.add(course.getUniqueId());
			}
			return coursesToApprove;
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	public List<ClassAssignmentInterface.Enrollment> listEnrollments(Long classOrOfferingId) throws SectioningException, PageAccessException {
		try {
			UserContext user = getSessionContext().getUser();
			if (user == null) throw new PageAccessException(
					getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());
			
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				InstructionalOffering offering = (classOrOfferingId >= 0 ? InstructionalOfferingDAO.getInstance().get(classOrOfferingId, hibSession) : null);
				Class_ clazz = (classOrOfferingId < 0 ? Class_DAO.getInstance().get(-classOrOfferingId, hibSession) : null);
				if (offering == null && clazz == null) 
					throw new SectioningException(MSG.exceptionBadClassOrOffering());
				if (offering == null)
					offering = clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering();
				Long offeringId = offering.getUniqueId();
				
				getSessionContext().checkPermission(offering, Right.OfferingEnrollments);

				OnlineSectioningServer server = getServerInstance(offering.getControllingCourseOffering().getSubjectArea().getSessionId(), false);
				
				if (server == null || !offering.isAllowStudentScheduling() || offering.isNotOffered() || offering.getInstrOfferingConfigs().isEmpty()) {
					NameFormat nameFormat = NameFormat.fromReference(ApplicationProperty.OnlineSchedulingStudentNameFormat.value());
					Map<String, String> approvedBy2name = new Hashtable<String, String>();
					Hashtable<Long, ClassAssignmentInterface.Enrollment> student2enrollment = new Hashtable<Long, ClassAssignmentInterface.Enrollment>();
					boolean canShowExtIds = sessionContext.hasPermission(Right.EnrollmentsShowExternalId);
					boolean canRegister = sessionContext.hasPermission(Right.CourseRequests);
					boolean canUseAssistant = sessionContext.hasPermission(Right.SchedulingAssistant);
					for (StudentClassEnrollment enrollment: (List<StudentClassEnrollment>)hibSession.createQuery(
							clazz == null ?
								"from StudentClassEnrollment e where e.courseOffering.instructionalOffering.uniqueId = :offeringId" :
								"select e from StudentClassEnrollment e where e.courseOffering.instructionalOffering.uniqueId = :offeringId and e.student.uniqueId in " +
								"(select f.student.uniqueId from StudentClassEnrollment f where f.clazz.uniqueId = " + clazz.getUniqueId() + ")"
							).setLong("offeringId", offeringId).list()) {
						ClassAssignmentInterface.Enrollment e = student2enrollment.get(enrollment.getStudent().getUniqueId());
						if (e == null) {
							ClassAssignmentInterface.Student st = new ClassAssignmentInterface.Student();
							st.setId(enrollment.getStudent().getUniqueId());
							st.setSessionId(enrollment.getStudent().getSession().getUniqueId());
							st.setExternalId(enrollment.getStudent().getExternalUniqueId());
							st.setCanShowExternalId(canShowExtIds);
							st.setCanRegister(canRegister);
							st.setCanUseAssistant(canUseAssistant);
							st.setName(nameFormat.format(enrollment.getStudent()));
							for (StudentAreaClassificationMajor acm: new TreeSet<StudentAreaClassificationMajor>(enrollment.getStudent().getAreaClasfMajors())) {
								st.addArea(acm.getAcademicArea().getAcademicAreaAbbreviation());
								st.addClassification(acm.getAcademicClassification().getCode());
								st.addMajor(acm.getMajor().getCode());
							}
							for (StudentGroup g: enrollment.getStudent().getGroups()) {
								st.addGroup(g.getGroupAbbreviation());
							}
			    			for (StudentAccomodation a: enrollment.getStudent().getAccomodations()) {
			    				st.addAccommodation(a.getAbbreviation());
			    			}
							e = new ClassAssignmentInterface.Enrollment();
							e.setStudent(st);
							e.setEnrolledDate(enrollment.getTimestamp());
							CourseAssignment c = new CourseAssignment();
							c.setCourseId(enrollment.getCourseOffering().getUniqueId());
							c.setSubject(enrollment.getCourseOffering().getSubjectAreaAbbv());
							c.setCourseNbr(enrollment.getCourseOffering().getCourseNbr());
							c.setTitle(enrollment.getCourseOffering().getTitle());
							c.setHasCrossList(enrollment.getCourseOffering().getInstructionalOffering().hasCrossList());
							e.setCourse(c);
							student2enrollment.put(enrollment.getStudent().getUniqueId(), e);
							if (enrollment.getCourseRequest() != null) {
								e.setPriority(1 + enrollment.getCourseRequest().getCourseDemand().getPriority());
								if (enrollment.getCourseRequest().getCourseDemand().getCourseRequests().size() > 1) {
									CourseRequest first = null;
									for (CourseRequest r: enrollment.getCourseRequest().getCourseDemand().getCourseRequests()) {
										if (first == null || r.getOrder().compareTo(first.getOrder()) < 0) first = r;
									}
									if (!first.equals(enrollment.getCourseRequest()))
										e.setAlternative(first.getCourseOffering().getCourseName());
								}
								if (enrollment.getCourseRequest().getCourseDemand().isAlternative()) {
									CourseDemand first = enrollment.getCourseRequest().getCourseDemand();
									demands: for (CourseDemand cd: enrollment.getStudent().getCourseDemands()) {
										if (!cd.isAlternative() && cd.getPriority().compareTo(first.getPriority()) < 0 && !cd.getCourseRequests().isEmpty()) {
											for (CourseRequest cr: cd.getCourseRequests())
												if (cr.getClassEnrollments().isEmpty()) continue demands;
											first = cd;
										}
									}
									CourseRequest alt = null;
									for (CourseRequest r: first.getCourseRequests()) {
										if (alt == null || r.getOrder().compareTo(alt.getOrder()) < 0) alt = r;
									}
									e.setAlternative(alt.getCourseOffering().getCourseName());
								}
								e.setRequestedDate(enrollment.getCourseRequest().getCourseDemand().getTimestamp());
								e.setApprovedDate(enrollment.getApprovedDate());
								if (enrollment.getApprovedBy() != null) {
									String name = approvedBy2name.get(enrollment.getApprovedBy());
									if (name == null) {
										TimetableManager mgr = (TimetableManager)hibSession.createQuery(
												"from TimetableManager where externalUniqueId = :externalId")
												.setString("externalId", enrollment.getApprovedBy())
												.setMaxResults(1).uniqueResult();
										if (mgr != null) {
											name = mgr.getName();
										} else {
											DepartmentalInstructor instr = (DepartmentalInstructor)hibSession.createQuery(
													"from DepartmentalInstructor where externalUniqueId = :externalId and department.session.uniqueId = :sessionId")
													.setString("externalId", enrollment.getApprovedBy())
													.setLong("sessionId", enrollment.getStudent().getSession().getUniqueId())
													.setMaxResults(1).uniqueResult();
											if (instr != null)
												name = instr.nameLastNameFirst();
										}
										if (name != null)
											approvedBy2name.put(enrollment.getApprovedBy(), name);
									}
									e.setApprovedBy(name == null ? enrollment.getApprovedBy() : name);
								}
								e.setWaitList(enrollment.getCourseRequest().getCourseDemand().isWaitlist());
							} else {
								e.setPriority(-1);
							}
						}
						ClassAssignmentInterface.ClassAssignment c = e.getCourse().addClassAssignment();
						c.setClassId(enrollment.getClazz().getUniqueId());
						c.setSection(enrollment.getClazz().getClassSuffix(enrollment.getCourseOffering()));
						if (c.getSection() == null)
							c.setSection(enrollment.getClazz().getSectionNumberString(hibSession));
						c.setClassNumber(enrollment.getClazz().getSectionNumberString(hibSession));
						c.setSubpart(enrollment.getClazz().getSchedulingSubpart().getItypeDesc());
					}
					if (classOrOfferingId >= 0)
						for (CourseRequest request: (List<CourseRequest>)hibSession.createQuery(
							"from CourseRequest r where r.courseOffering.instructionalOffering.uniqueId = :offeringId").setLong("offeringId", classOrOfferingId).list()) {
							ClassAssignmentInterface.Enrollment e = student2enrollment.get(request.getCourseDemand().getStudent().getUniqueId());
							if (e != null) continue;
							ClassAssignmentInterface.Student st = new ClassAssignmentInterface.Student();
							st.setId(request.getCourseDemand().getStudent().getUniqueId());
							st.setSessionId(request.getCourseDemand().getStudent().getSession().getUniqueId());
							st.setExternalId(request.getCourseDemand().getStudent().getExternalUniqueId());
							st.setCanShowExternalId(canShowExtIds);
							st.setCanRegister(canRegister);
							st.setCanUseAssistant(canUseAssistant);
							st.setName(nameFormat.format(request.getCourseDemand().getStudent()));
							for (StudentAreaClassificationMajor acm: new TreeSet<StudentAreaClassificationMajor>(request.getCourseDemand().getStudent().getAreaClasfMajors())) {
								st.addArea(acm.getAcademicArea().getAcademicAreaAbbreviation());
								st.addClassification(acm.getAcademicClassification().getCode());
								st.addMajor(acm.getMajor().getCode());
							}
							for (StudentGroup g: request.getCourseDemand().getStudent().getGroups()) {
								st.addGroup(g.getGroupAbbreviation());
							}
			    			for (StudentAccomodation a: request.getCourseDemand().getStudent().getAccomodations()) {
			    				st.addAccommodation(a.getAbbreviation());
			    			}
							e = new ClassAssignmentInterface.Enrollment();
							e.setStudent(st);
							CourseAssignment c = new CourseAssignment();
							c.setCourseId(request.getCourseOffering().getUniqueId());
							c.setSubject(request.getCourseOffering().getSubjectAreaAbbv());
							c.setCourseNbr(request.getCourseOffering().getCourseNbr());
							c.setTitle(request.getCourseOffering().getTitle());
							c.setHasCrossList(request.getCourseOffering().getInstructionalOffering().hasCrossList());
							e.setCourse(c);
							e.setWaitList(request.getCourseDemand().isWaitlist());
							student2enrollment.put(request.getCourseDemand().getStudent().getUniqueId(), e);
							e.setPriority(1 + request.getCourseDemand().getPriority());
							if (request.getCourseDemand().getCourseRequests().size() > 1) {
								CourseRequest first = null;
								for (CourseRequest r: request.getCourseDemand().getCourseRequests()) {
									if (first == null || r.getOrder().compareTo(first.getOrder()) < 0) first = r;
								}
								if (!first.equals(request))
									e.setAlternative(first.getCourseOffering().getCourseName());
							}
							if (request.getCourseDemand().isAlternative()) {
								CourseDemand first = request.getCourseDemand();
								demands: for (CourseDemand cd: request.getCourseDemand().getStudent().getCourseDemands()) {
									if (!cd.isAlternative() && cd.getPriority().compareTo(first.getPriority()) < 0 && !cd.getCourseRequests().isEmpty()) {
										for (CourseRequest cr: cd.getCourseRequests())
											if (cr.getClassEnrollments().isEmpty()) continue demands;
										first = cd;
									}
								}
								CourseRequest alt = null;
								for (CourseRequest r: first.getCourseRequests()) {
									if (alt == null || r.getOrder().compareTo(alt.getOrder()) < 0) alt = r;
								}
								e.setAlternative(alt.getCourseOffering().getCourseName());
							}
							e.setRequestedDate(request.getCourseDemand().getTimestamp());
						}
					return new ArrayList<ClassAssignmentInterface.Enrollment>(student2enrollment.values());
				} else {
					return server.execute(server.createAction(ListEnrollments.class)
							.forOffering(offeringId).withSection(clazz == null ? null : clazz.getUniqueId())
							.canShowExternalIds(sessionContext.hasPermission(Right.EnrollmentsShowExternalId))
							.canRegister(sessionContext.hasPermission(Right.CourseRequests))
							.canUseAssistant(sessionContext.hasPermission(Right.SchedulingAssistant)),
							currentUser());
				}
			} finally {
				hibSession.close();
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	public ClassAssignmentInterface getEnrollment(boolean online, Long studentId) throws SectioningException, PageAccessException {
		try {
			if (online) {
				getSessionContext().checkPermission(studentId, "Student", Right.StudentEnrollments);
				org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
				try {
					Student student = StudentDAO.getInstance().get(studentId, hibSession);
					if (student == null) 
						throw new SectioningException(MSG.exceptionBadStudentId());
					OnlineSectioningServer server = getServerInstance(student.getSession().getUniqueId(), false);
					if (server == null) {
						Comparator<StudentClassEnrollment> cmp = new Comparator<StudentClassEnrollment>() {
							public boolean isParent(SchedulingSubpart s1, SchedulingSubpart s2) {
								SchedulingSubpart p1 = s1.getParentSubpart();
								if (p1==null) return false;
								if (p1.equals(s2)) return true;
								return isParent(p1, s2);
							}

							@Override
							public int compare(StudentClassEnrollment a, StudentClassEnrollment b) {
								SchedulingSubpart s1 = a.getClazz().getSchedulingSubpart();
								SchedulingSubpart s2 = b.getClazz().getSchedulingSubpart();
								if (isParent(s1, s2)) return 1;
								if (isParent(s2, s1)) return -1;
								int cmp = s1.getItype().compareTo(s2.getItype());
								if (cmp != 0) return cmp;
								return Double.compare(s1.getUniqueId(), s2.getUniqueId());
							}
						};
						NameFormat nameFormat = NameFormat.fromReference(ApplicationProperty.OnlineSchedulingInstructorNameFormat.value());
						ClassAssignmentInterface ret = new ClassAssignmentInterface();
						Hashtable<Long, CourseAssignment> courses = new Hashtable<Long, ClassAssignmentInterface.CourseAssignment>();
						CourseCreditUnitConfig credit = null;
						Set<StudentClassEnrollment> enrollments = new TreeSet<StudentClassEnrollment>(cmp);
						enrollments.addAll(hibSession.createQuery(
								"from StudentClassEnrollment e where e.student.uniqueId = :studentId order by e.courseOffering.subjectAreaAbbv, e.courseOffering.courseNbr"
								).setLong("studentId", studentId).list());
						for (StudentClassEnrollment enrollment: enrollments) {
							CourseAssignment course = courses.get(enrollment.getCourseOffering().getUniqueId());
							if (course == null) {
								course = new CourseAssignment();
								courses.put(enrollment.getCourseOffering().getUniqueId(), course);
								ret.add(course);
								course.setAssigned(true);
								course.setCourseId(enrollment.getCourseOffering().getUniqueId());
								course.setCourseNbr(enrollment.getCourseOffering().getCourseNbr());
								course.setSubject(enrollment.getCourseOffering().getSubjectAreaAbbv());
								course.setTitle(enrollment.getCourseOffering().getTitle());
								course.setWaitListed(enrollment.getCourseRequest() != null && enrollment.getCourseRequest().getCourseDemand().getWaitlist() != null && enrollment.getCourseRequest().getCourseDemand().getWaitlist().booleanValue());
								course.setHasCrossList(enrollment.getCourseOffering().getInstructionalOffering().hasCrossList());
								credit = enrollment.getCourseOffering().getCredit();
								if (enrollment.getCourseRequest() != null)
									course.setRequestedDate(enrollment.getCourseRequest().getCourseDemand().getTimestamp());
							}
							ClassAssignment clazz = course.addClassAssignment();
							clazz.setClassId(enrollment.getClazz().getUniqueId());
							clazz.setCourseId(enrollment.getCourseOffering().getUniqueId());
							clazz.setCourseAssigned(true);
							clazz.setCourseNbr(enrollment.getCourseOffering().getCourseNbr());
							clazz.setTitle(enrollment.getCourseOffering().getTitle());
							clazz.setSubject(enrollment.getCourseOffering().getSubjectAreaAbbv());
							clazz.setSection(enrollment.getClazz().getClassSuffix(enrollment.getCourseOffering()));
							if (clazz.getSection() == null)
								clazz.setSection(enrollment.getClazz().getSectionNumberString(hibSession));
							clazz.setClassNumber(enrollment.getClazz().getSectionNumberString(hibSession));
							clazz.setSubpart(enrollment.getClazz().getSchedulingSubpart().getItypeDesc());
							if (enrollment.getClazz().getParentClass() != null) {
								clazz.setParentSection(enrollment.getClazz().getParentClass().getClassSuffix(enrollment.getCourseOffering()));
								if (clazz.getParentSection() == null)
									clazz.setParentSection(enrollment.getClazz().getParentClass().getSectionNumberString(hibSession));
							}
							if (enrollment.getClazz().getSchedulePrintNote() != null)
								clazz.addNote(enrollment.getClazz().getSchedulePrintNote());
							Placement placement = enrollment.getClazz().getCommittedAssignment() == null ? null : enrollment.getClazz().getCommittedAssignment().getPlacement();
							int minLimit = enrollment.getClazz().getExpectedCapacity();
		                	int maxLimit = enrollment.getClazz().getMaxExpectedCapacity();
		                	int limit = maxLimit;
		                	if (minLimit < maxLimit && placement != null) {
		                		// int roomLimit = Math.round((enrollment.getClazz().getRoomRatio() == null ? 1.0f : enrollment.getClazz().getRoomRatio()) * placement.getRoomSize());
		                		int roomLimit = (int) Math.floor(placement.getRoomSize() / (enrollment.getClazz().getRoomRatio() == null ? 1.0f : enrollment.getClazz().getRoomRatio()));
		                		limit = Math.min(Math.max(minLimit, roomLimit), maxLimit);
		                	}
		                    if (enrollment.getClazz().getSchedulingSubpart().getInstrOfferingConfig().isUnlimitedEnrollment() || limit >= 9999) limit = -1;
		                    clazz.setCancelled(enrollment.getClazz().isCancelled());
							clazz.setLimit(new int[] { enrollment.getClazz().getEnrollment(), limit});
							clazz.setEnrolledDate(enrollment.getTimestamp());
							if (placement != null) {
								if (placement.getTimeLocation() != null) {
									for (DayCode d : DayCode.toDayCodes(placement.getTimeLocation().getDayCode()))
										clazz.addDay(d.getIndex());
									clazz.setStart(placement.getTimeLocation().getStartSlot());
									clazz.setLength(placement.getTimeLocation().getLength());
									clazz.setBreakTime(placement.getTimeLocation().getBreakTime());
									clazz.setDatePattern(placement.getTimeLocation().getDatePatternName());
								}
								if (enrollment.getClazz().getCommittedAssignment() != null)
									for (Location loc: enrollment.getClazz().getCommittedAssignment().getRooms())
										clazz.addRoom(loc.getUniqueId(), loc.getLabelWithDisplayName());
								/*
								if (placement.getNrRooms() == 1) {
									clazz.addRoom(placement.getRoomLocation().getId(), placement.getRoomLocation().getName());
								} else if (placement.getNrRooms() > 1) {
									for (RoomLocation rm: placement.getRoomLocations())
										clazz.addRoom(rm.getId(), rm.getName());
								}
								*/
							}
							if (enrollment.getClazz().getDisplayInstructor())
								for (ClassInstructor ci : enrollment.getClazz().getClassInstructors()) {
									if (!ci.isLead()) continue;
									clazz.addInstructor(nameFormat.format(ci.getInstructor()));
									clazz.addInstructoEmail(ci.getInstructor().getEmail() == null ? "" : ci.getInstructor().getEmail());
								}
							if (enrollment.getClazz().getSchedulingSubpart().getCredit() != null) {
								clazz.setCredit(enrollment.getClazz().getSchedulingSubpart().getCredit().creditAbbv() + "|" + enrollment.getClazz().getSchedulingSubpart().getCredit().creditText());
							} else if (credit != null) {
								clazz.setCredit(credit.creditAbbv() + "|" + credit.creditText());
							}
							credit = null;
							if (clazz.getParentSection() == null)
								clazz.setParentSection(enrollment.getCourseOffering().getConsentType() == null ? null : enrollment.getCourseOffering().getConsentType().getLabel());
						}
						demands: for (CourseDemand demand: (List<CourseDemand>)hibSession.createQuery(
								"from CourseDemand d where d.student.uniqueId = :studentId order by d.priority"
								).setLong("studentId", studentId).list()) {
							if (demand.getFreeTime() != null) {
								CourseAssignment course = new CourseAssignment();
								course.setAssigned(true);
								ClassAssignment clazz = course.addClassAssignment();
								clazz.setLength(demand.getFreeTime().getLength());
								for (DayCode d: DayCode.toDayCodes(demand.getFreeTime().getDayCode()))
									clazz.addDay(d.getIndex());
								clazz.setStart(demand.getFreeTime().getStartSlot());
								ca: for (CourseAssignment ca: ret.getCourseAssignments()) {
									for (ClassAssignment c: ca.getClassAssignments()) {
										if (!c.isAssigned()) continue;
										for (int d: c.getDays())
											if (clazz.getDays().contains(d)) {
												if (c.getStart() + c.getLength() > clazz.getStart() && clazz.getStart() + clazz.getLength() > c.getStart()) {
													course.setAssigned(false);
													break ca;
												}
											}
									}
								}
								course.setRequestedDate(demand.getTimestamp());
								ret.add(course);
							} else {
								CourseRequest request = null;
								for (CourseRequest r: demand.getCourseRequests()) {
									if (courses.containsKey(r.getCourseOffering().getUniqueId())) continue demands;
									if (request == null || r.getOrder().compareTo(request.getOrder()) < 0)
										request = r;
								}
								if (request == null) continue;
								CourseAssignment course = new CourseAssignment();
								courses.put(request.getCourseOffering().getUniqueId(), course);
								course.setRequestedDate(demand.getTimestamp());
								ret.add(course);
								course.setAssigned(false);
								course.setWaitListed(demand.getWaitlist() != null && demand.getWaitlist().booleanValue());
								course.setCourseId(request.getCourseOffering().getUniqueId());
								course.setCourseNbr(request.getCourseOffering().getCourseNbr());
								course.setSubject(request.getCourseOffering().getSubjectAreaAbbv());
								course.setTitle(request.getCourseOffering().getTitle());
								course.setHasCrossList(request.getCourseOffering().getInstructionalOffering().hasCrossList());
								ClassAssignment clazz = course.addClassAssignment();
								clazz.setCourseId(request.getCourseOffering().getUniqueId());
								clazz.setCourseAssigned(false);
								clazz.setCourseNbr(request.getCourseOffering().getCourseNbr());
								clazz.setTitle(request.getCourseOffering().getTitle());
								clazz.setSubject(request.getCourseOffering().getSubjectAreaAbbv());
							}
						}
						
						ret.setRequest(getRequest(student));
						
						return ret;
					} else {
						return server.execute(server.createAction(GetAssignment.class).forStudent(studentId).withRequest(true).withCustomCheck(true), currentUser());
					}
				} finally {
					hibSession.close();
				}				
			} else {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());

				return server.execute(server.createAction(GetAssignment.class).forStudent(studentId).withRequest(true), currentUser());
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (AccessDeniedException e) {
			throw new PageAccessException(e.getMessage());
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	@Override
	public String approveEnrollments(Long classOrOfferingId, List<Long> studentIds) throws SectioningException, PageAccessException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			
			List<Long> courseIdsCanApprove = canApprove(classOrOfferingId);
			
			if (courseIdsCanApprove == null || courseIdsCanApprove.isEmpty())
				throw new SectioningException(MSG.exceptionInsufficientPrivileges());

			InstructionalOffering offering = (classOrOfferingId >= 0 ? InstructionalOfferingDAO.getInstance().get(classOrOfferingId, hibSession) : null);
			if (offering == null) {
				Class_ clazz = (classOrOfferingId < 0 ? Class_DAO.getInstance().get(-classOrOfferingId, hibSession) : null);
				if (clazz == null)
					throw new SectioningException(MSG.exceptionBadClassOrOffering());
				offering = clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering();
			}
			
			OnlineSectioningServer server = getServerInstance(offering.getControllingCourseOffering().getSubjectArea().getSessionId(), false);
			
			UserContext user = getSessionContext().getUser();
			String approval = new Date().getTime() + ":" + user.getTrueExternalUserId() + ":" + user.getTrueName();
			server.execute(server.createAction(ApproveEnrollmentsAction.class).withParams(offering.getUniqueId(), studentIds, courseIdsCanApprove, approval), currentUser());
			
			return approval;
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	@Override
	public Boolean rejectEnrollments(Long classOrOfferingId, List<Long> studentIds) throws SectioningException, PageAccessException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			
			List<Long> courseIdsCanApprove = canApprove(classOrOfferingId);
			if (courseIdsCanApprove == null || courseIdsCanApprove.isEmpty())
				throw new SectioningException(MSG.exceptionInsufficientPrivileges());
			
			InstructionalOffering offering = (classOrOfferingId >= 0 ? InstructionalOfferingDAO.getInstance().get(classOrOfferingId, hibSession) : null);
			if (offering == null) {
				Class_ clazz = (classOrOfferingId < 0 ? Class_DAO.getInstance().get(-classOrOfferingId, hibSession) : null);
				if (clazz == null)
					throw new SectioningException(MSG.exceptionBadClassOrOffering());
				offering = clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering();
			}
			
			OnlineSectioningServer server = getServerInstance(offering.getControllingCourseOffering().getSubjectArea().getSessionId(), false);
			
			UserContext user = getSessionContext().getUser();
			String approval = new Date().getTime() + ":" + user.getTrueExternalUserId() + ":" + user.getTrueName();
			
			return server.execute(server.createAction(RejectEnrollmentsAction.class).withParams(offering.getUniqueId(), studentIds, courseIdsCanApprove, approval), currentUser());
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	private Long getStatusPageSessionId() throws SectioningException, PageAccessException {
		UserContext user = getSessionContext().getUser();
		if (user == null)
			throw new PageAccessException(getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());
		if (user.getCurrentAcademicSessionId() == null) {
			Long sessionId = getLastSessionId();
			if (sessionId != null) return sessionId;
		} else {
			return user.getCurrentAcademicSessionId();
		}
		throw new SectioningException(MSG.exceptionNoAcademicSession());
	}
	
	private HashSet<Long> getCoordinatingCourses(Long sessionId) throws SectioningException, PageAccessException {
		UserContext user = getSessionContext().getUser();
		if (user == null)
			throw new PageAccessException(getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());

		if (getSessionContext().hasPermission(Right.HasRole)) return null; // only applies to users without a role
		
		HashSet<Long> courseIds = new HashSet<Long>(CourseOfferingDAO.getInstance().getSession().createQuery(
				"select distinct c.uniqueId from CourseOffering c inner join c.instructionalOffering.offeringCoordinators oc where " +
				"c.subjectArea.session.uniqueId = :sessionId and c.subjectArea.department.allowStudentScheduling = true and oc.instructor.externalUniqueId = :extId")
				.setLong("sessionId", sessionId).setString("extId", user.getExternalUserId()).setCacheable(true).list());
		
		return courseIds;
	}
	
	private Set<String> getSubjectAreas() throws SectioningException, PageAccessException {
		UserContext user = getSessionContext().getUser();
		if (user == null)
			throw new PageAccessException(getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());

		if (!getSessionContext().hasPermission(Right.HasRole) || getSessionContext().hasPermission(Right.DepartmentIndependent)) return null; // only applies to users with a role that is department dependent
		
		HashSet<String> subjects = new HashSet<String>();
		for (SubjectArea subject: SubjectArea.getUserSubjectAreas(user)) {
			subjects.add(subject.getSubjectAreaAbbreviation());
		}
		return subjects;
	}
	
	private HashSet<Long> getApprovableCourses(Long sessionId) throws SectioningException, PageAccessException {
		UserContext user = getSessionContext().getUser();
		if (user == null)
			throw new PageAccessException(getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());

		HashSet<Long> courseIds = new HashSet<Long>(CourseOfferingDAO.getInstance().getSession().createQuery(
				"select distinct c.uniqueId from CourseOffering c inner join c.instructionalOffering.offeringCoordinators oc where " +
				"c.subjectArea.session.uniqueId = :sessionId and c.subjectArea.department.allowStudentScheduling = true and c.consentType.reference = :reference and " +
				"oc.instructor.externalUniqueId = :extId"
				).setLong("sessionId", sessionId).setString("reference", "IN").setString("extId", user.getExternalUserId()).setCacheable(true).list());
		
		if (!user.getCurrentAuthority().hasRight(Right.HasRole)) return courseIds;
		
		if (user.getCurrentAuthority().hasRight(Right.SessionIndependent))
			return new HashSet<Long>(CourseOfferingDAO.getInstance().getSession().createQuery(
					"select c.uniqueId from CourseOffering c where c.subjectArea.session.uniqueId = :sessionId and c.subjectArea.department.allowStudentScheduling = true and c.consentType is not null"
					).setLong("sessionId", sessionId).setCacheable(true).list());
		
		for (Department d: Department.getUserDepartments(user)) {
			courseIds.addAll(CourseOfferingDAO.getInstance().getSession().createQuery(
					"select distinct c.uniqueId from CourseOffering c where " +
					"c.subjectArea.department.uniqueId = :departmentId and c.subjectArea.department.allowStudentScheduling = true and c.consentType is not null"
					).setLong("departmentId", d.getUniqueId()).setCacheable(true).list());
		}
		
		return courseIds;
	}
	
	private HashSet<Long> getMyStudents(Long sessionId) throws SectioningException, PageAccessException {
		UserContext user = getSessionContext().getUser();
		if (user == null || user.getCurrentAuthority() == null)
			throw new PageAccessException(getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());

		return new HashSet<Long>(CourseOfferingDAO.getInstance().getSession().createQuery(
				"select s.uniqueId from Advisor a inner join a.students s where " +
				"a.externalUniqueId = :user and a.role.reference = :role and a.session.uniqueId = :sessionId"
				).setLong("sessionId", sessionId).setString("user", user.getExternalUserId()).setString("role", user.getCurrentAuthority().getRole()).setCacheable(true).list());
	}
	
	public List<EnrollmentInfo> findEnrollmentInfos(boolean online, String query, SectioningStatusFilterRpcRequest filter, Long courseId) throws SectioningException, PageAccessException {
		try {
			if (online) {
				final Long sessionId = getStatusPageSessionId();
				
				OnlineSectioningServer server = getServerInstance(sessionId, true);
				if (server == null)
					throw new SectioningException(MSG.exceptionBadSession());
				
				if (server instanceof DatabaseServer) {
					return server.execute(server.createAction(DbFindEnrollmentInfoAction.class).withParams(
							query,
							courseId,
							getCoordinatingCourses(sessionId),
							query.matches("(?i:.*consent:[ ]?(todo|\\\"to do\\\").*)") ? getApprovableCourses(sessionId) : null,
							getMyStudents(sessionId),
							getSubjectAreas())
							.withFilter(filter), currentUser()
					);	
				}
							
				return server.execute(server.createAction(FindEnrollmentInfoAction.class).withParams(
						query,
						courseId,
						getCoordinatingCourses(sessionId),
						query.matches("(?i:.*consent:[ ]?(todo|\\\"to do\\\").*)") ? getApprovableCourses(sessionId) : null,
						getMyStudents(sessionId),
						getSubjectAreas())
						.withFilter(filter), currentUser()
				);				
			} else {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());

				return server.execute(server.createAction(FindEnrollmentInfoAction.class).withParams(query, courseId, null, null, getMyStudents(server.getAcademicSession().getUniqueId()), getSubjectAreas()).withFilter(filter), currentUser());
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	public List<ClassAssignmentInterface.StudentInfo> findStudentInfos(boolean online, String query, SectioningStatusFilterRpcRequest filter) throws SectioningException, PageAccessException {
		try {
			if (online) {
				Long sessionId = getStatusPageSessionId();
				
				OnlineSectioningServer server = getServerInstance(sessionId, true);
				if (server == null)
					throw new SectioningException(MSG.exceptionBadSession());
				
				if (server instanceof DatabaseServer) {
					return server.execute(server.createAction(DbFindStudentInfoAction.class).withParams(
							query,
							getCoordinatingCourses(sessionId),
							query.matches("(?i:.*consent:[ ]?(todo|\\\"to do\\\").*)") ? getApprovableCourses(sessionId) : null,
									getMyStudents(sessionId),
							getSubjectAreas(),
							sessionContext.hasPermission(Right.EnrollmentsShowExternalId),
							sessionContext.hasPermission(Right.CourseRequests),
							sessionContext.hasPermission(Right.SchedulingAssistant))
							.withFilter(filter)
							.withPermissions(getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdmin),
									getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor),
									getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents),
									getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)),
							currentUser()
					);
				}
				
				return server.execute(server.createAction(FindStudentInfoAction.class).withParams(
						query,
						getCoordinatingCourses(sessionId),
						query.matches("(?i:.*consent:[ ]?(todo|\\\"to do\\\").*)") ? getApprovableCourses(sessionId) : null,
						getMyStudents(sessionId),
						getSubjectAreas(),
						sessionContext.hasPermission(Right.EnrollmentsShowExternalId),
						sessionContext.hasPermission(Right.CourseRequests),
						sessionContext.hasPermission(Right.SchedulingAssistant))
						.withFilter(filter)
						.withPermissions(getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdmin),
								getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)),
						currentUser()
				);
			} else {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());

				return server.execute(server.createAction(FindStudentInfoAction.class).withParams(query, null, null, getMyStudents(server.getAcademicSession().getUniqueId()), getSubjectAreas(),
						sessionContext.hasPermission(Right.EnrollmentsShowExternalId), false, true).withFilter(filter)
						.withPermissions(getSessionContext().hasPermissionAnySession(server.getAcademicSession().getUniqueId(), Right.StudentSchedulingAdmin),
								getSessionContext().hasPermissionAnySession(server.getAcademicSession().getUniqueId(), Right.StudentSchedulingAdvisor),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)), currentUser());
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	public List<String[]> querySuggestions(boolean online, String query, int limit) throws SectioningException, PageAccessException {
		try {
			if (online) {
				Long sessionId = getStatusPageSessionId();
				
				OnlineSectioningServer server = getServerInstance(sessionId, true);
				if (server == null)
					throw new SectioningException(MSG.exceptionBadSession());
				
				UserContext user = getSessionContext().getUser();
				return server.execute(server.createAction(StatusPageSuggestionsAction.class).withParams(
						user.getExternalUserId(), user.getName(),
						query, limit), currentUser());				
			} else {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());

				UserContext user = getSessionContext().getUser();
				return server.execute(server.createAction(StatusPageSuggestionsAction.class).withParams(
						user.getExternalUserId(), user.getName(),
						query, limit), currentUser());				
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	@Override
	public List<org.unitime.timetable.gwt.shared.ClassAssignmentInterface.Enrollment> findEnrollments(
			boolean online, String query, SectioningStatusFilterRpcRequest filter, Long courseId, Long classId)
			throws SectioningException, PageAccessException {
		try {
			if (online) {
				Long sessionId = getStatusPageSessionId();
				
				OnlineSectioningServer server = getServerInstance(sessionId, true);
				if (server == null)
					throw new SectioningException(MSG.exceptionBadSession());
				
				if (getSessionContext().isAuthenticated())
					getSessionContext().getUser().setProperty("SectioningStatus.LastStatusQuery", query);
				
				if (server instanceof DatabaseServer) {
					return server.execute(server.createAction(DbFindEnrollmentAction.class).withParams(
							query, courseId, classId, 
							query.matches("(?i:.*consent:[ ]?(todo|\\\"to do\\\").*)") ? getApprovableCourses(sessionId).contains(courseId): false,
							sessionContext.hasPermission(Right.EnrollmentsShowExternalId),
							sessionContext.hasPermission(Right.CourseRequests),
							sessionContext.hasPermission(Right.SchedulingAssistant),
							getMyStudents(sessionId)).withFilter(filter)
							.withPermissions(getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdmin),
									getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor),
									getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents),
									getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)),
							currentUser());
				}
				
				return server.execute(server.createAction(FindEnrollmentAction.class).withParams(
						query, courseId, classId, 
						query.matches("(?i:.*consent:[ ]?(todo|\\\"to do\\\").*)") ? getApprovableCourses(sessionId).contains(courseId): false,
						sessionContext.hasPermission(Right.EnrollmentsShowExternalId),
						sessionContext.hasPermission(Right.CourseRequests),
						sessionContext.hasPermission(Right.SchedulingAssistant),
						getMyStudents(sessionId)).withFilter(filter)
						.withPermissions(getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdmin),
								getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)),
						currentUser());
			} else {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());
				
				if (getSessionContext().isAuthenticated())
					getSessionContext().getUser().setProperty("SectioningStatus.LastStatusQuery", query);
				
				return server.execute(server.createAction(FindEnrollmentAction.class).withParams(
						query, courseId, classId, false,
						sessionContext.hasPermission(Right.EnrollmentsShowExternalId), false, true,
						getMyStudents(server.getAcademicSession().getUniqueId())).withFilter(filter)
						.withPermissions(getSessionContext().hasPermissionAnySession(server.getAcademicSession().getUniqueId(), Right.StudentSchedulingAdmin),
								getSessionContext().hasPermissionAnySession(server.getAcademicSession().getUniqueId(), Right.StudentSchedulingAdvisor),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents),
								getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)),
						currentUser());
			}
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch  (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	@Override
	public Long canEnroll(boolean online, Long studentId) throws SectioningException, PageAccessException {
		return canEnroll(online, null, studentId);
	}
	
	protected Long canEnroll(boolean online, Long sessionId, Long studentId) throws SectioningException, PageAccessException {
		try {
			if (!online) {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					throw new SectioningException(MSG.exceptionNoSolver());
				
				CourseRequestInterface request = server.execute(server.createAction(GetRequest.class).forStudent(studentId), currentUser());
				if (request == null)
					throw new SectioningException(MSG.exceptionBadStudentId());

				return server.getAcademicSession().getUniqueId();
			}
			
			boolean recheckCustomEligibility = ApplicationProperty.OnlineSchedulingCustomEligibilityRecheck.isTrue();
			if (!recheckCustomEligibility) {
				EligibilityCheck last = (EligibilityCheck)getSessionContext().getAttribute("eligibility");
				if (last != null && (last.hasFlag(EligibilityFlag.RECHECK_BEFORE_ENROLLMENT) || !last.hasFlag(EligibilityFlag.CAN_ENROLL)))
					recheckCustomEligibility = true;
			}
			
			EligibilityCheck check = checkEligibility(online, true, sessionId, studentId, null, recheckCustomEligibility);
			if (check == null || !check.hasFlag(EligibilityFlag.CAN_ENROLL) || check.hasFlag(EligibilityFlag.RECHECK_BEFORE_ENROLLMENT))
				throw new SectioningException(check.getMessage() == null ?
						check.hasFlag(EligibilityFlag.PIN_REQUIRED) ? MSG.exceptionAuthenticationPinNotProvided() :
						MSG.exceptionInsufficientPrivileges() : check.getMessage()).withEligibilityCheck(check);
			
			if (studentId.equals(getStudentId(check.getSessionId())))
				return check.getSessionId();
			
			if (getSessionContext().hasPermissionAnySession(check.getSessionId(), Right.StudentSchedulingAdvisor))
				return check.getSessionId();
			
			OnlineSectioningServer server = getServerInstance(check.getSessionId(), false);
			if (server == null)
				throw new SectioningException(MSG.exceptionNoServerForSession());
			
			if (getStudentId(check.getSessionId()) == null)
				throw new PageAccessException(MSG.exceptionEnrollNotStudent(server.getAcademicSession().toString()));
			
			throw new PageAccessException(MSG.exceptionInsufficientPrivileges());
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	protected CourseRequestInterface getRequest(Student student) {
		CourseRequestInterface request = new CourseRequestInterface();
		request.setAcademicSessionId(student.getSession().getUniqueId());
		request.setStudentId(student.getUniqueId());
		request.setSaved(true);
		request.setMaxCredit(student.getMaxCredit());
		if (student.getOverrideMaxCredit() != null) {
			request.setMaxCreditOverride(student.getOverrideMaxCredit());
			request.setMaxCreditOverrideExternalId(student.getOverrideExternalId());
			request.setMaxCreditOverrideTimeStamp(student.getOverrideTimeStamp());
			Integer status = student.getOverrideStatus();
			if (status == null)
				request.setMaxCreditOverrideStatus(RequestedCourseStatus.OVERRIDE_PENDING);
			else if (status == org.unitime.timetable.model.CourseRequest.CourseRequestOverrideStatus.APPROVED.ordinal())
				request.setMaxCreditOverrideStatus(RequestedCourseStatus.OVERRIDE_APPROVED);
			else if (status == org.unitime.timetable.model.CourseRequest.CourseRequestOverrideStatus.REJECTED.ordinal())
				request.setMaxCreditOverrideStatus(RequestedCourseStatus.OVERRIDE_REJECTED);
			else if (status == org.unitime.timetable.model.CourseRequest.CourseRequestOverrideStatus.CANCELLED.ordinal())
				request.setMaxCreditOverrideStatus(RequestedCourseStatus.OVERRIDE_CANCELLED);
			else
				request.setMaxCreditOverrideStatus(RequestedCourseStatus.OVERRIDE_PENDING);
		}
		Set<Long> courseIds = new HashSet<Long>();
		if (!student.getCourseDemands().isEmpty()) {
			TreeSet<CourseDemand> demands = new TreeSet<CourseDemand>(new Comparator<CourseDemand>() {
				public int compare(CourseDemand d1, CourseDemand d2) {
					if (d1.isAlternative() && !d2.isAlternative()) return 1;
					if (!d1.isAlternative() && d2.isAlternative()) return -1;
					int cmp = d1.getPriority().compareTo(d2.getPriority());
					if (cmp != 0) return cmp;
					return d1.getUniqueId().compareTo(d2.getUniqueId());
				}
			});
			demands.addAll(student.getCourseDemands());
			CourseRequestInterface.Request lastRequest = null;
			int lastRequestPriority = -1;
			for (CourseDemand cd: demands) {
				CourseRequestInterface.Request r = null;
				if (cd.getFreeTime() != null) {
					CourseRequestInterface.FreeTime ft = new CourseRequestInterface.FreeTime();
					ft.setStart(cd.getFreeTime().getStartSlot());
					ft.setLength(cd.getFreeTime().getLength());
					for (DayCode day : DayCode.toDayCodes(cd.getFreeTime().getDayCode()))
						ft.addDay(day.getIndex());
					if (lastRequest != null && lastRequestPriority == cd.getPriority() && lastRequest.hasRequestedCourse() && lastRequest.getRequestedCourse(0).isFreeTime()) {
						lastRequest.getRequestedCourse(0).addFreeTime(ft);
					} else {
						r = new CourseRequestInterface.Request();
						RequestedCourse rc = new RequestedCourse();
						rc.addFreeTime(ft);
						r.addRequestedCourse(rc);
						if (cd.isAlternative())
							request.getAlternatives().add(r);
						else
							request.getCourses().add(r);
						lastRequest = r;
						lastRequestPriority = cd.getPriority();
						rc.setStatus(RequestedCourseStatus.SAVED);
					}
				} else if (!cd.getCourseRequests().isEmpty()) {
					r = new CourseRequestInterface.Request();
					for (CourseRequest course: new TreeSet<CourseRequest>(cd.getCourseRequests())) {
						courseIds.add(course.getCourseOffering().getUniqueId());
						RequestedCourse rc = new RequestedCourse();
						rc.setCourseId(course.getCourseOffering().getUniqueId());
						rc.setCourseName(course.getCourseOffering().getSubjectAreaAbbv() + " " + course.getCourseOffering().getCourseNbr() + (!CONSTANTS.showCourseTitle() ? "" : " - " + course.getCourseOffering().getTitle()));
						rc.setCourseTitle(course.getCourseOffering().getTitle());
						CourseCreditUnitConfig credit = course.getCourseOffering().getCredit(); 
						if (credit != null) rc.setCredit(credit.getMinCredit(), credit.getMaxCredit());
						boolean hasEnrollments = !course.getClassEnrollments().isEmpty(); 
						rc.setReadOnly(hasEnrollments);
						rc.setCanDelete(!hasEnrollments);
						if (hasEnrollments)
							rc.setStatus(RequestedCourseStatus.ENROLLED);
						else if (course.getOverrideStatus() != null)
							rc.setStatus(course.isRequestApproved() ? RequestedCourseStatus.OVERRIDE_APPROVED : course.isRequestRejected() ? RequestedCourseStatus.OVERRIDE_REJECTED : course.isRequestCancelled() ? RequestedCourseStatus.OVERRIDE_CANCELLED : RequestedCourseStatus.OVERRIDE_PENDING);
						else
							rc.setStatus(RequestedCourseStatus.SAVED);
						rc.setOverrideExternalId(course.getOverrideExternalId());
						rc.setOverrideTimeStamp(course.getOverrideTimeStamp());
						CourseRequestOption pref = course.getCourseRequestOption(OnlineSectioningLog.CourseRequestOption.OptionType.REQUEST_PREFERENCE);
						if (pref != null) {
							try {
								OnlineSectioningHelper.fillPreferencesIn(rc, pref.getOption());
							} catch (InvalidProtocolBufferException e) {}
						}
						r.addRequestedCourse(rc);
					}
					if (r.hasRequestedCourse()) {
						if (cd.isAlternative())
							request.getAlternatives().add(r);
						else
							request.getCourses().add(r);
					}
					r.setWaitList(cd.getWaitlist());
					r.setTimeStamp(cd.getTimestamp());
					lastRequest = r;
					lastRequestPriority = cd.getPriority();
				}
			}
		}
		if (!student.getClassEnrollments().isEmpty()) {
			TreeSet<CourseOffering> courses = new TreeSet<CourseOffering>();
			for (Iterator<StudentClassEnrollment> i = student.getClassEnrollments().iterator(); i.hasNext(); ) {
				StudentClassEnrollment enrl = i.next();
				if (courseIds.contains(enrl.getCourseOffering().getUniqueId())) continue;
				courses.add(enrl.getCourseOffering());
			}
			for (CourseOffering c: courses) {
				CourseRequestInterface.Request r = new CourseRequestInterface.Request();
				RequestedCourse rc = new RequestedCourse();
				rc.setCourseId(c.getUniqueId());
				rc.setCourseName(c.getSubjectAreaAbbv() + " " + c.getCourseNbr() + (!CONSTANTS.showCourseTitle() ? "" : " - " + c.getTitle()));
				rc.setCourseTitle(c.getTitle());
				CourseCreditUnitConfig credit = c.getCredit(); 
				if (credit != null) rc.setCredit(credit.getMinCredit(), credit.getMaxCredit());
				r.addRequestedCourse(rc);
				request.getCourses().add(r);
				rc.setReadOnly(true); rc.setCanDelete(false); rc.setStatus(RequestedCourseStatus.ENROLLED);
			}
		}
		
		if (CustomCourseRequestsValidationHolder.hasProvider()) {
			OnlineSectioningServer server = getServerInstance(student.getSession().getUniqueId(), true);
			if (server != null) {
				try {
					return server.execute(server.createAction(CustomCourseRequestsValidationHolder.Check.class).withRequest(request), currentUser());
				} catch (SectioningException e) {
					sLog.warn("Failed to validate course requests: " + e.getMessage(), e);
				}
			}
		}
		
		return request;
	}

	@Override
	public CourseRequestInterface savedRequest(boolean online, boolean sectioning, Long sessionId, Long studentId) throws SectioningException, PageAccessException {
		if (studentId == null) {
			studentId = getStudentId(sessionId);
			if (studentId == null && sessionId != null && online && CustomCourseRequestsHolder.hasProvider()) {
				OnlineSectioningServer server = getServerInstance(sessionId, false);
				if (server != null)
					return server.execute(server.createAction(GetRequest.class), currentUser());
			}
			if (studentId == null) throw new SectioningException(MSG.exceptionNoStudent());
		}
		if (!online) {
			OnlineSectioningServer server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());
			return server.execute(server.createAction(GetRequest.class).forStudent(studentId, sectioning), currentUser());
		}
		OnlineSectioningServer server = getServerInstance(sessionId == null ? canEnroll(online, studentId) : sessionId, false);
		if (server != null) {
			return server.execute(server.createAction(GetRequest.class).forStudent(studentId, sectioning).withCustomValidation(!sectioning), currentUser());
		} else {
			org.hibernate.Session hibSession = StudentDAO.getInstance().getSession();
			try {
				Student student = StudentDAO.getInstance().get(studentId, hibSession);
				if (student == null) throw new SectioningException(MSG.exceptionBadStudentId());
				return getRequest(student);
			} catch (PageAccessException e) {
				throw e;
			} catch (SectioningException e) {
				throw e;
			} catch (Exception e) {
				sLog.error(e.getMessage(), e);
				throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
			} finally {
				hibSession.close();
			}
		}		
	}

	@Override
	public ClassAssignmentInterface savedResult(boolean online, Long sessionId, Long studentId) throws SectioningException, PageAccessException {
		if (online) {
			OnlineSectioningServer server = getServerInstance(sessionId == null ? canEnroll(online, studentId) : sessionId, false);
			return server.execute(server.createAction(GetAssignment.class).forStudent(studentId), currentUser());
		} else {
			OnlineSectioningServer server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());

			ClassAssignmentInterface ret = server.execute(server.createAction(GetAssignment.class).forStudent(studentId), currentUser());
			if (ret != null)
				ret.setCanEnroll(getStudentId(sessionId) != null);
			return ret;
		}
	}
	
	@Override
	public Boolean selectSession(Long sessionId) {
		getSessionContext().setAttribute("sessionId", sessionId);
		UserContext user = getSessionContext().getUser();
		if (user != null && user.getCurrentAuthority() != null) {
			List<? extends UserAuthority> authorities = user.getAuthorities(user.getCurrentAuthority().getRole(), new SimpleQualifier("Session", sessionId));
			if (!authorities.isEmpty()) user.setCurrentAuthority(authorities.get(0));
			else user.setCurrentAuthority(null);
		}
		return true;
	}
	
	private StudentStatusInfo toStudentStatusInfo(StudentSectioningStatus status, List<CourseType> types) {
		StudentStatusInfo info = new StudentStatusInfo();
		info.setUniqueId(status.getUniqueId());
		info.setReference(status.getReference());
		info.setLabel(status.getLabel());
		info.setCanAccessAssistantPage(status.hasOption(StudentSectioningStatus.Option.enabled));
		info.setCanAccessRequestsPage(status.hasOption(StudentSectioningStatus.Option.regenabled));
		info.setCanStudentEnroll(status.hasOption(StudentSectioningStatus.Option.enrollment));
		info.setCanStudentRegister(status.hasOption(StudentSectioningStatus.Option.registration));
		info.setCanAdvisorEnroll(status.hasOption(StudentSectioningStatus.Option.advisor));
		info.setCanAdvisorRegister(status.hasOption(StudentSectioningStatus.Option.regadvisor));
		info.setCanAdminEnroll(status.hasOption(StudentSectioningStatus.Option.admin));
		info.setCanAdminRegister(status.hasOption(StudentSectioningStatus.Option.regadmin));
		info.setWaitList(status.hasOption(StudentSectioningStatus.Option.waitlist));
		info.setEmail(status.hasOption(StudentSectioningStatus.Option.email));
		info.setMessage(status.getMessage());
		if (!status.hasOption(Option.notype)) { // all but
			Set<String> prohibited = new TreeSet<String>();
			for (CourseType type: types)
				if (status.getTypes() == null || !status.getTypes().contains(type))
					prohibited.add(type.getReference());
			if (!prohibited.isEmpty()) {
				String refs = "";
				for (String ref: prohibited)
					refs += (refs.isEmpty() ? "" : ", ") + ref; 
				info.setCourseTypes(MSG.courseTypesAllBut(refs));
			}
		} else {
			Set<String> allowed = new TreeSet<String>();
			for (CourseType type: status.getTypes())
				allowed.add(type.getReference());
			if (allowed.isEmpty()) {
				info.setCourseTypes(MSG.courseTypesNoneAllowed());
			} else {
				String refs = "";
				for (String ref: allowed)
					refs += (refs.isEmpty() ? "" : ", ") + ref;
				info.setCourseTypes(MSG.courseTypesAllowed(refs));
			}
		}
		if (status.getEffectiveStartDate() != null || status.getEffectiveStartPeriod() != null) {
			if (status.getEffectiveStartDate() == null)
				info.setEffectiveStart(Constants.slot2str(status.getEffectiveStartPeriod()));
			else if (status.getEffectiveStartPeriod() == null)
				info.setEffectiveStart(Formats.getDateFormat(Formats.Pattern.DATE_EVENT).format(status.getEffectiveStartDate()));
			else
				info.setEffectiveStart(Formats.getDateFormat(Formats.Pattern.DATE_EVENT).format(status.getEffectiveStartDate()) + " " + Constants.slot2str(status.getEffectiveStartPeriod()));
		}
		if (status.getEffectiveStopDate() != null || status.getEffectiveStopPeriod() != null) {
			if (status.getEffectiveStopDate() == null)
				info.setEffectiveStop(Constants.slot2str(status.getEffectiveStopPeriod()));
			else if (status.getEffectiveStopPeriod() == null)
				info.setEffectiveStop(Formats.getDateFormat(Formats.Pattern.DATE_EVENT).format(status.getEffectiveStopDate()));
			else
				info.setEffectiveStop(Formats.getDateFormat(Formats.Pattern.DATE_EVENT).format(status.getEffectiveStopDate()) + " " + Constants.slot2str(status.getEffectiveStopPeriod()));
		}
		return info;
	}

	@Override
	public List<StudentStatusInfo> lookupStudentSectioningStates() throws SectioningException, PageAccessException {
		List<CourseType> courseTypes = CourseTypeDAO.getInstance().getSession().createQuery(
				"select distinct t from CourseOffering c inner join c.courseType t where c.instructionalOffering.session = :sessionId order by t.reference"
				).setLong("sessionId", getStatusPageSessionId()).setCacheable(true).list();
		List<StudentStatusInfo> ret = new ArrayList<StudentStatusInfo>();
		boolean advisor = (getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdvisor) &&
				!getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdmin));
		boolean email = ApplicationProperty.OnlineSchedulingEmailConfirmation.isTrue();
		boolean waitlist = CustomStudentEnrollmentHolder.isAllowWaitListing();
		if (!advisor) {
			Session session = SessionDAO.getInstance().get(getStatusPageSessionId());
			StudentStatusInfo info = null;
			if (session.getDefaultSectioningStatus() != null) {
				info = toStudentStatusInfo(session.getDefaultSectioningStatus(), courseTypes);
				info.setUniqueId(null);
				info.setReference("");
				info.setLabel(MSG.studentStatusSessionDefault(session.getDefaultSectioningStatus().getLabel()));
				info.setEffectiveStart(null); info.setEffectiveStop(null);
			} else {
				info = new StudentStatusInfo();
				info.setReference("");
				info.setLabel(MSG.studentStatusSystemDefault());
				info.setAllEnabled();
			}
			if (!email) info.setEmail(false);
			if (!waitlist) info.setWaitList(false);
			ret.add(info);
		}
		for (StudentSectioningStatus s: StudentSectioningStatusDAO.getInstance().findAll()) {
			if (s.isPast()) continue;
			if (advisor && !s.hasOption(StudentSectioningStatus.Option.advcanset)) continue;
			StudentStatusInfo info = toStudentStatusInfo(s, courseTypes);
			if (!email) info.setEmail(false);
			if (!waitlist) info.setWaitList(false);
			ret.add(info);
		}
		return ret;
	}

	@Override
	public Boolean sendEmail(Long studentId, String subject, String message, String cc) throws SectioningException, PageAccessException {
		try {
			OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), true);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			if (ApplicationProperty.OnlineSchedulingEmailConfirmation.isFalse())
				throw new SectioningException(MSG.exceptionStudentEmailsDisabled());
			getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingEmailStudent);
			
			if (!getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdmin) &&
					getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdvisor) &&
					!getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)) {
					getSessionContext().checkPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents);
					Set<Long> myStudentIds = getMyStudents(getStatusPageSessionId());
					if (!myStudentIds.contains(studentId)) {
						Student student = StudentDAO.getInstance().get(studentId);
						throw new PageAccessException(SEC_MSG.permissionCheckFailed(Right.StudentSchedulingEmailStudent.toString(),
								(student == null ? studentId.toString() : student.getName(NameFormat.LAST_FIRST_MIDDLE.reference()))));
					}
				}
			
			StudentEmail email = server.createAction(StudentEmail.class).forStudent(studentId);
			email.setCC(cc);
			email.setEmailSubject(subject == null || subject.isEmpty() ? MSG.defaulSubject() : subject);
			email.setMessage(message);
			return server.execute(email, currentUser());
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	@Override
	public Boolean changeStatus(List<Long> studentIds, String note, String ref) throws SectioningException, PageAccessException {
		try {
			OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), true);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingChangeStudentStatus);
			if (!getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdmin) &&
				getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdvisor) &&
				!getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)) {
				getSessionContext().checkPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents);
				Set<Long> myStudentIds = getMyStudents(getStatusPageSessionId());
				for (Long studentId: studentIds) {
					if (!myStudentIds.contains(studentId)) {
						Student student = StudentDAO.getInstance().get(studentId);
						throw new PageAccessException(SEC_MSG.permissionCheckFailed(Right.StudentSchedulingChangeStudentStatus.toString(),
								(student == null ? studentId.toString() : student.getName(NameFormat.LAST_FIRST_MIDDLE.reference()))));
					}
				}
			}
			Boolean ret = server.execute(server.createAction(ChangeStudentStatus.class).forStudents(studentIds).withStatus(ref).withNote(note), currentUser());
			try {
		        SessionFactory hibSessionFactory = SessionDAO.getInstance().getSession().getSessionFactory();
		        for (Long studentId: studentIds)
		        	hibSessionFactory.getCache().evictEntity(Student.class, studentId);
	        } catch (Exception e) {
	        	sLog.warn("Failed to evict cache: " + e.getMessage());
	        }
			return ret;
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	@Override
	public Boolean changeStudentGroup(List<Long> studentIds, Long groupId, boolean remove) throws SectioningException, PageAccessException {
		try {
			OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), true);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingChangeStudentGroup);
			if (!getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdmin) &&
				getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdvisor) &&
				!getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)) {
				getSessionContext().checkPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents);
				Set<Long> myStudentIds = getMyStudents(getStatusPageSessionId());
				for (Long studentId: studentIds) {
					if (!myStudentIds.contains(studentId)) {
						Student student = StudentDAO.getInstance().get(studentId);
						throw new PageAccessException(SEC_MSG.permissionCheckFailed(Right.StudentSchedulingChangeStudentGroup.toString(),
								(student == null ? studentId.toString() : student.getName(NameFormat.LAST_FIRST_MIDDLE.reference()))));
					}
				}
			}
			return server.execute(server.createAction(ChangeStudentGroup.class).forStudents(studentIds).withGroup(groupId, remove), currentUser());
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}

	}
	
	private OnlineSectioningLog.Entity currentUser() {
		UserContext user = getSessionContext().getUser();
		UniTimePrincipal principal = (UniTimePrincipal)getSessionContext().getAttribute("user");
		String pin = (String)getSessionContext().getAttribute("pin");
		String specialRequestId = (String)getSessionContext().getAttribute("specreq");
		if (user != null) {
			OnlineSectioningLog.Entity.Builder entity = OnlineSectioningLog.Entity.newBuilder()
					.setExternalId(user.getTrueExternalUserId())
					.setName(user.getTrueName() == null ? user.getUsername() : user.getTrueName())
					.setType(user instanceof Chameleon || principal != null ?
							OnlineSectioningLog.Entity.EntityType.MANAGER : OnlineSectioningLog.Entity.EntityType.STUDENT);
			if (pin != null) entity.addParameterBuilder().setKey("pin").setValue(pin);
			if (principal != null && principal.getStudentExternalId() != null) entity.addParameterBuilder().setKey("student").setValue(principal.getStudentExternalId());
			if (specialRequestId != null) entity.addParameterBuilder().setKey("specreq").setValue(specialRequestId);
			return entity.build();
		} else if (principal != null) {
			OnlineSectioningLog.Entity.Builder entity = OnlineSectioningLog.Entity.newBuilder()
					.setExternalId(principal.getExternalId())
					.setName(principal.getName())
					.setType(OnlineSectioningLog.Entity.EntityType.MANAGER);
			if (pin != null) entity.addParameterBuilder().setKey("pin").setValue(pin);
			if (specialRequestId != null) entity.addParameterBuilder().setKey("specreq").setValue(specialRequestId);
			return entity.build();
		} else {
			return null;
		}
		
	}
	
	@Override
	public List<SectioningAction> changeLog(String query) throws SectioningException, PageAccessException {
		Long sessionId = getStatusPageSessionId();
		OnlineSectioningServer server = getServerInstance(sessionId, true);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		if (server instanceof DatabaseServer)
			return server.execute(server.createAction(DbFindOnlineSectioningLogAction.class).forQuery(query), currentUser());
		return server.execute(server.createAction(FindOnlineSectioningLogAction.class).forQuery(query), currentUser());
	}

	@Override
	public Boolean massCancel(List<Long> studentIds, String statusRef, String subject, String message, String cc) throws SectioningException, PageAccessException {
		try {
			OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), false);
			if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
			
			getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingMassCancel);
			
			if (!getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdmin) &&
					getSessionContext().hasPermissionAnySession(getStatusPageSessionId(), Right.StudentSchedulingAdvisor) &&
					!getSessionContext().hasPermission(Right.StudentSchedulingAdvisorCanModifyAllStudents)) {
					getSessionContext().checkPermission(Right.StudentSchedulingAdvisorCanModifyMyStudents);
					Set<Long> myStudentIds = getMyStudents(getStatusPageSessionId());
					for (Long studentId: studentIds) {
						if (!myStudentIds.contains(studentId)) {
							Student student = StudentDAO.getInstance().get(studentId);
							throw new PageAccessException(SEC_MSG.permissionCheckFailed(Right.StudentSchedulingMassCancel.toString(),
									(student == null ? studentId.toString() : student.getName(NameFormat.LAST_FIRST_MIDDLE.reference()))));
						}
					}
				}
			
			return server.execute(server.createAction(MassCancelAction.class).forStudents(studentIds).withStatus(statusRef).withEmail(subject, message, cc), currentUser());
		} catch (PageAccessException e) {
			throw e;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}

	static class CourseMatcher extends AbstractCourseMatcher {
		private org.unitime.timetable.onlinesectioning.match.CourseMatcher iParent;
		private static final long serialVersionUID = 1L;
		private boolean iAllCourseTypes, iNoCourseType;
		private Set<String> iAllowedCourseTypes;
		
		public CourseMatcher(boolean allCourseTypes, boolean noCourseType, Set<String> allowedCourseTypes) {
			iAllCourseTypes = allCourseTypes; iNoCourseType = noCourseType; iAllowedCourseTypes = allowedCourseTypes;
		}
		
		public boolean isAllCourseTypes() { return iAllCourseTypes; }
		
		public boolean isNoCourseType() { return iNoCourseType; }
		
		public boolean hasAllowedCourseTypes() { return iAllowedCourseTypes != null && !iAllowedCourseTypes.isEmpty(); }
		
		public Set<String> getAllowedCourseTypes() { return iAllowedCourseTypes; }
		
		public org.unitime.timetable.onlinesectioning.match.CourseMatcher getParentCourseMatcher() { return iParent; }
		
		public void setParentCourseMatcher(org.unitime.timetable.onlinesectioning.match.CourseMatcher parent) { iParent = parent; }

		@Override
		public boolean match(XCourseId course) {
			return course != null && course.matchType(iAllCourseTypes, iNoCourseType, iAllowedCourseTypes) && (iParent == null || iParent.match(course));
		}
	}

	@Override
	public EligibilityCheck checkEligibility(boolean online, boolean sectioning, Long sessionId, Long studentId, String pin) throws SectioningException, PageAccessException {
		return checkEligibility(online, sectioning, sessionId, studentId, pin, true);
	}
	
	public EligibilityCheck checkEligibility(boolean online, boolean sectioning, Long sessionId, Long studentId, String pin, boolean includeCustomCheck) throws SectioningException, PageAccessException {
		try {
			if (pin != null && !pin.isEmpty()) getSessionContext().setAttribute("pin", pin);
			if (includeCustomCheck) getSessionContext().removeAttribute("eligibility");
			
			if (!online) {
				OnlineSectioningServer server = getStudentSolver();
				if (server == null) 
					return new EligibilityCheck(MSG.exceptionNoSolver());
				
				EligibilityCheck check = new EligibilityCheck();
				check.setSessionId(server.getAcademicSession().getUniqueId());
				check.setStudentId(studentId);
				check.setFlag(EligibilityFlag.CAN_USE_ASSISTANT, true);
				check.setFlag(EligibilityFlag.CAN_ENROLL, true);
				check.setFlag(EligibilityFlag.CAN_WAITLIST, true);
				check.setFlag(EligibilityFlag.CAN_RESET, true);
				check.setFlag(EligibilityFlag.CONFIRM_DROP, false);
				check.setFlag(EligibilityFlag.QUICK_ADD_DROP, ApplicationProperty.OnlineSchedulingQuickAddDrop.isTrue());
				check.setFlag(EligibilityFlag.ALTERNATIVES_DROP, ApplicationProperty.OnlineSchedulingAlternativesDrop.isTrue());
				check.setFlag(EligibilityFlag.GWT_CONFIRMATIONS, ApplicationProperty.OnlineSchedulingGWTConfirmations.isTrue());
				check.setFlag(EligibilityFlag.DEGREE_PLANS, CustomDegreePlansHolder.hasProvider());
				check.setFlag(EligibilityFlag.NO_REQUEST_ARROWS, ApplicationProperty.OnlineSchedulingNoRequestArrows.isTrue());
				return check;
			}
			
			if (sessionId == null && studentId != null) {
				// guess session from student
				Student student = StudentDAO.getInstance().get(studentId);
				if (student != null)
					sessionId = student.getSession().getUniqueId();
			}
			if (sessionId == null) {
				// use last used session otherwise
				sessionId = getLastSessionId();
			} else {
				setLastSessionId(sessionId);
			}
			
			if (sessionId == null) return new EligibilityCheck(MSG.exceptionNoAcademicSession());
			
			UserContext user = getSessionContext().getUser();
			if (user == null)
				return new EligibilityCheck(getSessionContext().isHttpSessionNew() ? MSG.exceptionHttpSessionExpired() : MSG.exceptionLoginRequired());

			if (studentId == null)
				studentId = getStudentId(sessionId);
			
			EligibilityCheck check = new EligibilityCheck();
			check.setFlag(EligibilityFlag.IS_ADMIN, getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdmin));
			check.setFlag(EligibilityFlag.IS_ADVISOR, getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor));
			check.setFlag(EligibilityFlag.IS_GUEST, user instanceof AnonymousUserContext);
			check.setFlag(EligibilityFlag.CAN_RESET, ApplicationProperty.OnlineSchedulingAllowScheduleReset.isTrue());
			if (!check.hasFlag(EligibilityFlag.CAN_RESET) && (check.hasFlag(EligibilityFlag.IS_ADMIN) || check.hasFlag(EligibilityFlag.IS_ADVISOR)))
				check.setFlag(EligibilityFlag.CAN_RESET, ApplicationProperty.OnlineSchedulingAllowScheduleResetIfAdmin.isTrue());
			check.setFlag(EligibilityFlag.CONFIRM_DROP, ApplicationProperty.OnlineSchedulingConfirmCourseDrop.isTrue());
			check.setFlag(EligibilityFlag.QUICK_ADD_DROP, ApplicationProperty.OnlineSchedulingQuickAddDrop.isTrue());
			check.setFlag(EligibilityFlag.ALTERNATIVES_DROP, ApplicationProperty.OnlineSchedulingAlternativesDrop.isTrue());
			check.setFlag(EligibilityFlag.GWT_CONFIRMATIONS, ApplicationProperty.OnlineSchedulingGWTConfirmations.isTrue());
			check.setFlag(EligibilityFlag.DEGREE_PLANS, CustomDegreePlansHolder.hasProvider());
			check.setFlag(EligibilityFlag.NO_REQUEST_ARROWS, ApplicationProperty.OnlineSchedulingNoRequestArrows.isTrue());
			check.setSessionId(sessionId);
			check.setStudentId(studentId);
			
			if (!sectioning) {
				OnlineSectioningServer server = getServerInstance(sessionId, true);
				if (server == null) {
					Student student = (studentId == null ? null : StudentDAO.getInstance().get(studentId));
					if (student == null) {
						if (!check.hasFlag(EligibilityFlag.IS_ADMIN) && !check.hasFlag(EligibilityFlag.IS_ADVISOR))
							check.setMessage(MSG.exceptionEnrollNotStudent(SessionDAO.getInstance().get(sessionId).getLabel()));
						return check;
					}
					StudentSectioningStatus status = student.getEffectiveStatus();
					if (status == null) {
						check.setFlag(EligibilityFlag.CAN_USE_ASSISTANT, true);
						check.setFlag(EligibilityFlag.CAN_WAITLIST, true);
					} else {
						check.setFlag(EligibilityFlag.CAN_USE_ASSISTANT, status.hasOption(StudentSectioningStatus.Option.regenabled));
						check.setFlag(EligibilityFlag.CAN_WAITLIST, status.hasOption(StudentSectioningStatus.Option.waitlist));
						check.setMessage(status.getMessage());
					}
					check.setFlag(EligibilityFlag.CAN_REGISTER, getSessionContext().hasPermissionAnySession(student, Right.StudentSchedulingCanRegister));
					
					if (!check.hasMessage() && !check.hasFlag(EligibilityFlag.CAN_REGISTER))
						check.setMessage(MSG.exceptionAccessDisabled());
					return check;
				} else {
					return server.execute(server.createAction(CourseRequestEligibility.class).forStudent(studentId).withCheck(check).includeCustomCheck(includeCustomCheck)
							.withPermission(getSessionContext().hasPermissionAnySession(studentId, "Student", Right.StudentSchedulingCanRegister)), currentUser());
				}
			}
			
			OnlineSectioningServer server = getServerInstance(sessionId, false);
			if (server == null)
				return new EligibilityCheck(MSG.exceptionNoServerForSession());
			
			EligibilityCheck ret = server.execute(server.createAction(CheckEligibility.class).forStudent(studentId).withCheck(check).includeCustomCheck(includeCustomCheck)
					.withPermission(getSessionContext().hasPermissionAnySession(studentId, "Student", Right.StudentSchedulingCanEnroll)), currentUser());
			if (includeCustomCheck) getSessionContext().setAttribute("eligibility", ret);
			
			return ret;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			return new EligibilityCheck(MSG.exceptionUnknown(e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName()));
		}
	}

	@Override
	public void destroy() throws Exception {
		CustomStudentEnrollmentHolder.release();
		CustomCourseRequestsHolder.release();
		CustomDegreePlansHolder.release();
		CustomSpecialRegistrationHolder.release();
		CustomCourseRequestsValidationHolder.release();
	}

	@Override
	public SectioningProperties getProperties(Long sessionId) throws SectioningException, PageAccessException {
		SectioningProperties properties = new SectioningProperties();
		properties.setAdmin(getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdmin));
		properties.setAdvisor(getSessionContext().hasPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor));
		if (sessionId == null && getSessionContext().getUser() != null)
			sessionId = getSessionContext().getUser().getCurrentAcademicSessionId();
		properties.setSessionId(sessionId);
		if (sessionId != null) {
			properties.setChangeLog(properties.isAdmin() && getServerInstance(sessionId, true) != null);
			properties.setMassCancel(getSessionContext().hasPermission(sessionId, Right.StudentSchedulingMassCancel));
			properties.setEmail(getSessionContext().hasPermission(sessionId, Right.StudentSchedulingEmailStudent));
			properties.setChangeStatus(getSessionContext().hasPermission(sessionId, Right.StudentSchedulingChangeStudentStatus));
			properties.setRequestUpdate(getSessionContext().hasPermission(sessionId, Right.StudentSchedulingRequestStudentUpdate));
			properties.setCheckStudentOverrides(getSessionContext().hasPermission(sessionId, Right.StudentSchedulingCheckStudentOverrides));
			properties.setValidateStudentOverrides(getSessionContext().hasPermission(sessionId, Right.StudentSchedulingValidateStudentOverrides));
			if (getSessionContext().hasPermission(sessionId, Right.StudentSchedulingChangeStudentGroup))
				for (StudentGroup g: (List<StudentGroup>)StudentGroupDAO.getInstance().getSession().createQuery(
						"from StudentGroup g where g.type.advisorsCanSet = true and g.session = :sessionId order by g.groupAbbreviation"
						).setLong("sessionId", sessionId).setCacheable(true).list()) {
					properties.addEditableGroup(new StudentGroupInfo(g.getUniqueId(), g.getGroupAbbreviation(), g.getGroupName()));
				}
		}
		return properties;
	}

	@Override
	public Boolean requestStudentUpdate(List<Long> studentIds) throws SectioningException, PageAccessException {
		OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), false);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		
		getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingRequestStudentUpdate);
		
		return server.execute(server.createAction(RequestStudentUpdates.class).forStudents(studentIds), currentUser());
	}
	
	@Override
	public Boolean checkStudentOverrides(List<Long> studentIds) throws SectioningException, PageAccessException {
		OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), true);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		
		getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingCheckStudentOverrides);
		
		return server.execute(server.createAction(CustomCourseRequestsValidationHolder.Update.class).forStudents(studentIds), currentUser());
	}
	
	@Override
	public Boolean validateStudentOverrides(List<Long> studentIds) throws SectioningException, PageAccessException {
		OnlineSectioningServer server = getServerInstance(getStatusPageSessionId(), true);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		
		getSessionContext().checkPermission(server.getAcademicSession(), Right.StudentSchedulingValidateStudentOverrides);
		
		return server.execute(server.createAction(CustomCourseRequestsValidationHolder.Validate.class).forStudents(studentIds), currentUser());
	}

	@Override
	public List<DegreePlanInterface> listDegreePlans(boolean online, Long sessionId, Long studentId) throws SectioningException, PageAccessException {
		if (sessionId == null)
			sessionId = getLastSessionId();
		if (studentId == null)
			studentId = getStudentId(sessionId);
		else if (!studentId.equals(getStudentId(sessionId)))
			getSessionContext().checkPermissionAnySession(sessionId, Right.StudentSchedulingAdvisor);
		
		OnlineSectioningServer server = null;
		if (!online) {
			server = getStudentSolver();
			if (server == null) 
				throw new SectioningException(MSG.exceptionNoSolver());
		} else {
			server = getServerInstance(sessionId, true);
			if (server == null)
				throw new SectioningException(MSG.exceptionNoServerForSession());
		}
		
		return server.execute(server.createAction(GetDegreePlans.class).forStudent(studentId), currentUser());
	}

	@Override
	public ClassAssignmentInterface.Student lookupStudent(boolean online, String studentId) throws SectioningException, PageAccessException {
		if (getSessionContext().getUser() == null || getSessionContext().getUser().getCurrentAcademicSessionId() == null) return null;
		Student student = Student.findByExternalId(getSessionContext().getUser().getCurrentAcademicSessionId(), studentId);
		if (student == null) return null;
		getSessionContext().checkPermission(student, Right.StudentEnrollments);
		StudentSectioningStatus status = student.getEffectiveStatus();
		ClassAssignmentInterface.Student st = new ClassAssignmentInterface.Student();
		st.setId(student.getUniqueId());
		st.setSessionId(getSessionContext().getUser().getCurrentAcademicSessionId());
		st.setExternalId(student.getExternalUniqueId());
		st.setCanShowExternalId(getSessionContext().hasPermission(Right.EnrollmentsShowExternalId));
		st.setCanRegister((status == null || status.hasOption(StudentSectioningStatus.Option.regenabled)) && getSessionContext().hasPermission(Right.CourseRequests) && (getSessionContext().hasPermission(Right.StudentSchedulingAdmin) || getSessionContext().hasPermission(Right.StudentSchedulingAdvisor)));
		st.setCanUseAssistant(online
				? (status == null || status.hasOption(StudentSectioningStatus.Option.enabled)) && getSessionContext().hasPermission(Right.SchedulingAssistant) && (getSessionContext().hasPermission(Right.StudentSchedulingAdmin) || getSessionContext().hasPermission(Right.StudentSchedulingAdvisor))
				: getStudentSolver() != null);
		st.setName(student.getName(ApplicationProperty.OnlineSchedulingStudentNameFormat.value()));
		for (StudentAreaClassificationMajor acm: new TreeSet<StudentAreaClassificationMajor>(student.getAreaClasfMajors())) {
			st.addArea(acm.getAcademicArea().getAcademicAreaAbbreviation());
			st.addClassification(acm.getAcademicClassification().getCode());
			st.addMajor(acm.getMajor().getCode());
		}
		for (StudentGroup g: student.getGroups()) {
			st.addGroup(g.getGroupAbbreviation());
		}
		for (StudentAccomodation a: student.getAccomodations()) {
			st.addAccommodation(a.getAbbreviation());
		}
		return st;
	}

	@Override
	public RetrieveSpecialRegistrationResponse retrieveSpecialRequest(RetrieveSpecialRegistrationRequest request) throws SectioningException, PageAccessException {
		if (request.getSessionId() == null) {
			request.setSessionId(getLastSessionId());
		}
		if (request.getStudentId() == null) {
			Long sessionId = request.getSessionId();
			if (sessionId == null) sessionId = getLastSessionId();
			if (sessionId != null) request.setStudentId(getStudentId(sessionId));
		}
		
		if (request.getSessionId() == null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		if (request.getStudentId() == null) throw new SectioningException(MSG.exceptionNoStudent());
		
		OnlineSectioningServer server = getServerInstance(request.getSessionId(), false);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		if (!server.getAcademicSession().isSectioningEnabled() || !CustomSpecialRegistrationHolder.hasProvider())
			throw new SectioningException(MSG.exceptionNotSupportedFeature());
		
		setLastSessionId(request.getSessionId());

		return server.execute(server.createAction(SpecialRegistrationRetrieve.class).withRequest(request), currentUser());
	}

	@Override
	public SubmitSpecialRegistrationResponse submitSpecialRequest(SubmitSpecialRegistrationRequest request) throws SectioningException, PageAccessException {
		if (request.getSessionId() == null) {
			request.setSessionId(getLastSessionId());
		}
		if (request.getStudentId() == null) {
			Long sessionId = request.getSessionId();
			if (sessionId == null) sessionId = getLastSessionId();
			if (sessionId != null) request.setStudentId(getStudentId(sessionId));
		}
		
		if (request.getSessionId() == null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		if (request.getStudentId() == null) throw new SectioningException(MSG.exceptionNoStudent());
		
		OnlineSectioningServer server = getServerInstance(request.getSessionId(), false);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		if (!server.getAcademicSession().isSectioningEnabled() || !CustomSpecialRegistrationHolder.hasProvider())
			throw new SectioningException(MSG.exceptionNotSupportedFeature());
		
		setLastSessionId(request.getSessionId());

		return server.execute(server.createAction(SpecialRegistrationSubmit.class).withRequest(request), currentUser());
	}

	@Override
	public SpecialRegistrationEligibilityResponse checkSpecialRequestEligibility(SpecialRegistrationEligibilityRequest request) throws SectioningException, PageAccessException {
		if (request.getSessionId() == null) {
			request.setSessionId(getLastSessionId());
		}
		if (request.getStudentId() == null) {
			Long sessionId = request.getSessionId();
			if (sessionId == null) sessionId = getLastSessionId();
			if (sessionId != null) request.setStudentId(getStudentId(sessionId));
		}
		
		if (request.getSessionId() == null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		if (request.getStudentId() == null) throw new SectioningException(MSG.exceptionNoStudent());
		
		OnlineSectioningServer server = getServerInstance(request.getSessionId(), false);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		if (!server.getAcademicSession().isSectioningEnabled() || !CustomSpecialRegistrationHolder.hasProvider())
			throw new SectioningException(MSG.exceptionNotSupportedFeature());
		
		setLastSessionId(request.getSessionId());

		return server.execute(server.createAction(SpecialRegistrationEligibility.class).withRequest(request), currentUser());
	}

	@Override
	public List<RetrieveSpecialRegistrationResponse> retrieveAllSpecialRequests(RetrieveAllSpecialRegistrationsRequest request) throws SectioningException, PageAccessException {
		if (request.getSessionId() == null) {
			request.setSessionId(getLastSessionId());
		}
		if (request.getStudentId() == null) {
			Long sessionId = request.getSessionId();
			if (sessionId == null) sessionId = getLastSessionId();
			if (sessionId != null) request.setStudentId(getStudentId(sessionId));
		}
		
		if (request.getSessionId() == null) throw new SectioningException(MSG.exceptionNoAcademicSession());
		if (request.getStudentId() == null) throw new SectioningException(MSG.exceptionNoStudent());
		
		OnlineSectioningServer server = getServerInstance(request.getSessionId(), false);
		if (server == null) throw new SectioningException(MSG.exceptionNoServerForSession());
		if (!server.getAcademicSession().isSectioningEnabled() || !CustomSpecialRegistrationHolder.hasProvider())
			throw new SectioningException(MSG.exceptionNotSupportedFeature());
		
		setLastSessionId(request.getSessionId());

		return server.execute(server.createAction(SpecialRegistrationRetrieveAll.class).withRequest(request), currentUser());
	}

	@Override
	public org.unitime.timetable.gwt.shared.ClassAssignmentInterface.Student lookupStudent(boolean online, Long studentId) throws SectioningException, PageAccessException {
		if (getSessionContext().getUser() == null || getSessionContext().getUser().getCurrentAcademicSessionId() == null) return null;
		Student student = StudentDAO.getInstance().get(studentId);
		if (student == null) return null;
		getSessionContext().checkPermission(student, Right.StudentEnrollments);
		StudentSectioningStatus status = student.getEffectiveStatus();
		ClassAssignmentInterface.Student st = new ClassAssignmentInterface.Student();
		st.setId(student.getUniqueId());
		st.setSessionId(getSessionContext().getUser().getCurrentAcademicSessionId());
		st.setExternalId(student.getExternalUniqueId());
		st.setCanShowExternalId(getSessionContext().hasPermission(Right.EnrollmentsShowExternalId));
		st.setCanRegister((status == null || status.hasOption(StudentSectioningStatus.Option.regenabled)) && getSessionContext().hasPermission(Right.CourseRequests) && (getSessionContext().hasPermission(Right.StudentSchedulingAdmin) || getSessionContext().hasPermission(Right.StudentSchedulingAdvisor)));
		st.setCanUseAssistant(online
				? (status == null || status.hasOption(StudentSectioningStatus.Option.enabled)) && getSessionContext().hasPermission(Right.SchedulingAssistant) && (getSessionContext().hasPermission(Right.StudentSchedulingAdmin) || getSessionContext().hasPermission(Right.StudentSchedulingAdvisor))
				: getStudentSolver() != null);
		st.setName(student.getName(ApplicationProperty.OnlineSchedulingStudentNameFormat.value()));
		for (StudentAreaClassificationMajor acm: new TreeSet<StudentAreaClassificationMajor>(student.getAreaClasfMajors())) {
			st.addArea(acm.getAcademicArea().getAcademicAreaAbbreviation());
			st.addClassification(acm.getAcademicClassification().getCode());
			st.addMajor(acm.getMajor().getCode());
		}
		for (StudentGroup g: student.getGroups()) {
			st.addGroup(g.getGroupAbbreviation());
		}
		for (StudentAccomodation a: student.getAccomodations()) {
			st.addAccommodation(a.getAbbreviation());
		}
		return st;
	}
}