package models.workers;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import play.data.validation.ValidationError;
import services.ErrorMessages;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Worker if a study is run as standalone (not from MTurk)
 * 
 * @author Kristian Lange
 */
@Entity
@DiscriminatorValue(StandaloneWorker.WORKER_TYPE)
public class StandaloneWorker extends Worker {

	public static final String WORKER_TYPE = "Standalone";
	public static final String COMMENT = "comment";

	private String comment;
	
	public StandaloneWorker() {
	}
	
	@JsonCreator
	public StandaloneWorker(String commment) {
		this.comment = commment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return this.comment;
	}
	
	@Override
	public String toString() {
		return comment + ", " + super.toString();
	}

	@Override
	public String generateConfirmationCode() {
		return null;
	}
	
	public List<ValidationError> validate() {
		List<ValidationError> errorList = new ArrayList<ValidationError>();
		if (comment != null && !Jsoup.isValid(comment, Whitelist.none())) {
			errorList.add(new ValidationError(COMMENT,
					ErrorMessages.NO_HTML_ALLOWED));
		}
		return errorList.isEmpty() ? null : errorList;
	}

}
