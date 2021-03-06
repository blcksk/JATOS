package models.common;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import models.common.workers.Worker;

/**
 * Domain model /entity of a study result. Used for JSON marshalling and JPA
 * persistance.
 * 
 * @author Kristian Lange
 */
@Entity
@Table(name = "StudyResult")
@JsonPropertyOrder(value = { "id", "startDate", "worker", "confirmationCode",
		"studyState", "errorMsg", "abortMsg" })
public class StudyResult {

	@Id
	@GeneratedValue
	private Long id;

	/**
	 * Time and date when the study was started on the server.
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
	private Timestamp startDate;

	/**
	 * Time and date when the study was finished on the server.
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
	private Timestamp endDate;

	public enum StudyState {
		STARTED, // Study was started
		DATA_RETRIEVED, // Study's jsonData were retrieved
		FINISHED, // Study successfully finished
		ABORTED, // Study aborted by worker
		FAIL; // Something went wrong
		public static String allStatesAsString() {
			String str = Arrays.toString(values());
			return str.substring(1, str.length() - 1);
		}
	}

	/**
	 * State in the progress of a study. (Yes, it should be named
	 * studyResultState - but hey, it's so much nice this way.)
	 */
	private StudyState studyState;

	/**
	 * Temporary, global data storage that can be used by components to exchange
	 * data while the study is running. It will be deleted after the study is
	 * finished. It's stored as a normal string but jatos.js is converting it
	 * into JSON. It's initialised with an empty JSON object.
	 */
	@Lob
	private String studySessionData = "{}";

	/**
	 * Study this StudyResult belongs to. This relationship is unidirectional.
	 */
	@JsonIgnore
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_id")
	private Study study;

	/**
	 * Batch this StudyResult belongs to. This relationship is unidirectional.
	 */
	@JsonIgnore
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "batch_id")
	private Batch batch;

	/**
	 * List of ComponentResults that belongs to this StudyResult. This
	 * relationship is bidirectional.
	 */
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY)
	@OrderColumn(name = "componentResultList_order")
	@JoinColumn(name = "studyResult_id")
	// Not using mappedBy because of
	// http://stackoverflow.com/questions/2956171/jpa-2-0-ordercolumn-annotation-in-hibernate-3-5
	private List<ComponentResult> componentResultList = new ArrayList<>();

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_id", insertable = false, updatable = false, nullable = false)
	private Worker worker;

	/**
	 * GroupResult this StudyResult belongs to if the corresponding study is a
	 * group study. This relationship is bidirectional.
	 */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activeGroupMember_id")
	private GroupResult activeGroupResult;

	/**
	 * After the group study is finished the active GroupResult moves here.
	 * Before this field is null. This relationship is bidirectional.
	 */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "historyGroupMember_id")
	private GroupResult historyGroupResult;

	private String confirmationCode;

	/**
	 * Error message in case something went wrong with the study (state is
	 * FAIL). Can be left null.
	 */
	private String errorMsg;

	/**
	 * Message in case the study was aborted (state is ABORTED). Can be left
	 * null.
	 */
	private String abortMsg;

	public StudyResult() {
	}

	public StudyResult(Study study, Batch batch) {
		this.startDate = new Timestamp(new Date().getTime());
		this.study = study;
		this.batch = batch;
		this.studyState = StudyState.STARTED;
	}

	@JsonProperty("workerId")
	public Long getWorkerId() {
		return worker.getId();
	}

	@JsonProperty("workerType")
	public String getWorkerType() {
		return worker.getWorkerType();
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	public void setStartDate(Timestamp startDate) {
		this.startDate = startDate;
	}

	public Timestamp getStartDate() {
		return this.startDate;
	}

	public void setEndDate(Timestamp endDate) {
		this.endDate = endDate;
	}

	public Timestamp getEndDate() {
		return this.endDate;
	}

	public void setStudyState(StudyState state) {
		this.studyState = state;
	}

	public StudyState getStudyState() {
		return this.studyState;
	}

	public void setStudySessionData(String studySessionData) {
		this.studySessionData = studySessionData;
	}

	public String getStudySessionData() {
		return this.studySessionData;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public Study getStudy() {
		return this.study;
	}

	public void setBatch(Batch batch) {
		this.batch = batch;
	}

	public Batch getBatch() {
		return this.batch;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	public void setAbortMsg(String abortMsg) {
		this.abortMsg = abortMsg;
	}

	public String getAbortMsg() {
		return this.abortMsg;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getConfirmationCode() {
		return this.confirmationCode;
	}

	public void setComponentResultList(
			List<ComponentResult> componentResultList) {
		this.componentResultList = componentResultList;
	}

	public List<ComponentResult> getComponentResultList() {
		return this.componentResultList;
	}

	public void removeComponentResult(ComponentResult componentResult) {
		componentResultList.remove(componentResult);
	}

	public void addComponentResult(ComponentResult componentResult) {
		componentResultList.add(componentResult);
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	public Worker getWorker() {
		return this.worker;
	}

	public GroupResult getActiveGroupResult() {
		return activeGroupResult;
	}

	public void setActiveGroupResult(GroupResult groupResult) {
		this.activeGroupResult = groupResult;
	}

	public GroupResult getHistoryGroupResult() {
		return historyGroupResult;
	}

	public void setHistoryGroupResult(GroupResult groupResult) {
		this.historyGroupResult = groupResult;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof StudyResult)) {
			return false;
		}
		StudyResult other = (StudyResult) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.getId())) {
			return false;
		}
		return true;
	}

}
