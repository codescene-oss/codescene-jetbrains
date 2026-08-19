
package com.codescene.data.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "old-score",
    "new-score",
    "score-change",
    "file-level-findings",
    "function-level-findings"
})
@Generated("jsonschema2pojo")
public class Delta {

    /**
     * If the file was not recently created, the old file score
     * 
     */
    @JsonProperty("old-score")
    @JsonPropertyDescription("If the file was not recently created, the old file score")
    private Double oldScore;
    /**
     * If file is still present, the new score for the file
     * 
     */
    @JsonProperty("new-score")
    @JsonPropertyDescription("If file is still present, the new score for the file")
    private Double newScore;
    /**
     * Represents the change in score for this Delta. An empty old- or new score is assumed to be 10.0 when comparing.
     * (Required)
     * 
     */
    @JsonProperty("score-change")
    @JsonPropertyDescription("Represents the change in score for this Delta. An empty old- or new score is assumed to be 10.0 when comparing.")
    private Double scoreChange;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-level-findings")
    private List<ChangeDetail> fileLevelFindings = new ArrayList<ChangeDetail>();
    /**
     * Function level findings also include expression level smells
     *    (i.e. Complex Conditionals). For expression level smells the 'function' range might only correspond to the highlighting
     *    range - unless the function also contains other smells.
     * (Required)
     * 
     */
    @JsonProperty("function-level-findings")
    @JsonPropertyDescription("Function level findings also include expression level smells\n   (i.e. Complex Conditionals). For expression level smells the 'function' range might only correspond to the highlighting\n   range - unless the function also contains other smells.")
    private List<FunctionFinding> functionLevelFindings = new ArrayList<FunctionFinding>();
    @JsonProperty("old-git-blob-sha")
    private String oldGitBlobSha;
    @JsonProperty("new-git-blob-sha")
    private String newGitBlobSha;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Delta() {
    }

    /**
     * 
     * @param oldScore
     *     If the file was not recently created, the old file score.
     * @param scoreChange
     *     Represents the change in score for this Delta. An empty old- or new score is assumed to be 10.0 when comparing.
     * @param newScore
     *     If file is still present, the new score for the file.
     * @param functionLevelFindings
     *     Function level findings also include expression level smells
     *        (i.e. Complex Conditionals). For expression level smells the 'function' range might only correspond to the highlighting
     *        range - unless the function also contains other smells.
     */
    public Delta(Double oldScore, Double newScore, Double scoreChange, List<ChangeDetail> fileLevelFindings, List<FunctionFinding> functionLevelFindings) {
        super();
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.scoreChange = scoreChange;
        this.fileLevelFindings = fileLevelFindings;
        this.functionLevelFindings = functionLevelFindings;
    }

    /**
     * If the file was not recently created, the old file score
     * 
     */
    @JsonProperty("old-score")
    public Optional<Double> getOldScore() {
        return Optional.ofNullable(oldScore);
    }

    /**
     * If the file was not recently created, the old file score
     * 
     */
    @JsonProperty("old-score")
    public void setOldScore(Double oldScore) {
        this.oldScore = oldScore;
    }

    public Delta withOldScore(Double oldScore) {
        this.oldScore = oldScore;
        return this;
    }

    /**
     * If file is still present, the new score for the file
     * 
     */
    @JsonProperty("new-score")
    public Optional<Double> getNewScore() {
        return Optional.ofNullable(newScore);
    }

    /**
     * If file is still present, the new score for the file
     * 
     */
    @JsonProperty("new-score")
    public void setNewScore(Double newScore) {
        this.newScore = newScore;
    }

    public Delta withNewScore(Double newScore) {
        this.newScore = newScore;
        return this;
    }

    /**
     * Represents the change in score for this Delta. An empty old- or new score is assumed to be 10.0 when comparing.
     * (Required)
     * 
     */
    @JsonProperty("score-change")
    public Double getScoreChange() {
        return scoreChange;
    }

    /**
     * Represents the change in score for this Delta. An empty old- or new score is assumed to be 10.0 when comparing.
     * (Required)
     * 
     */
    @JsonProperty("score-change")
    public void setScoreChange(Double scoreChange) {
        this.scoreChange = scoreChange;
    }

    public Delta withScoreChange(Double scoreChange) {
        this.scoreChange = scoreChange;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-level-findings")
    public List<ChangeDetail> getFileLevelFindings() {
        return fileLevelFindings;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-level-findings")
    public void setFileLevelFindings(List<ChangeDetail> fileLevelFindings) {
        this.fileLevelFindings = fileLevelFindings;
    }

    public Delta withFileLevelFindings(List<ChangeDetail> fileLevelFindings) {
        this.fileLevelFindings = fileLevelFindings;
        return this;
    }

    /**
     * Function level findings also include expression level smells
     *    (i.e. Complex Conditionals). For expression level smells the 'function' range might only correspond to the highlighting
     *    range - unless the function also contains other smells.
     * (Required)
     * 
     */
    @JsonProperty("function-level-findings")
    public List<FunctionFinding> getFunctionLevelFindings() {
        return functionLevelFindings;
    }

    /**
     * Function level findings also include expression level smells
     *    (i.e. Complex Conditionals). For expression level smells the 'function' range might only correspond to the highlighting
     *    range - unless the function also contains other smells.
     * (Required)
     * 
     */
    @JsonProperty("function-level-findings")
    public void setFunctionLevelFindings(List<FunctionFinding> functionLevelFindings) {
        this.functionLevelFindings = functionLevelFindings;
    }

    public Delta withFunctionLevelFindings(List<FunctionFinding> functionLevelFindings) {
        this.functionLevelFindings = functionLevelFindings;
        return this;
    }

    @JsonProperty("old-git-blob-sha")
    public String getOldGitBlobSha() {
        return oldGitBlobSha;
    }

    @JsonProperty("old-git-blob-sha")
    public void setOldGitBlobSha(String oldGitBlobSha) {
        this.oldGitBlobSha = oldGitBlobSha;
    }

    @JsonProperty("new-git-blob-sha")
    public String getNewGitBlobSha() {
        return newGitBlobSha;
    }

    @JsonProperty("new-git-blob-sha")
    public void setNewGitBlobSha(String newGitBlobSha) {
        this.newGitBlobSha = newGitBlobSha;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.fileLevelFindings == null)? 0 :this.fileLevelFindings.hashCode()));
        result = ((result* 31)+((this.functionLevelFindings == null)? 0 :this.functionLevelFindings.hashCode()));
        result = ((result* 31)+((this.oldScore == null)? 0 :this.oldScore.hashCode()));
        result = ((result* 31)+((this.scoreChange == null)? 0 :this.scoreChange.hashCode()));
        result = ((result* 31)+((this.newScore == null)? 0 :this.newScore.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Delta) == false) {
            return false;
        }
        Delta rhs = ((Delta) other);
        return ((((((this.fileLevelFindings == rhs.fileLevelFindings)||((this.fileLevelFindings!= null)&&this.fileLevelFindings.equals(rhs.fileLevelFindings)))&&((this.functionLevelFindings == rhs.functionLevelFindings)||((this.functionLevelFindings!= null)&&this.functionLevelFindings.equals(rhs.functionLevelFindings))))&&((this.oldScore == rhs.oldScore)||((this.oldScore!= null)&&this.oldScore.equals(rhs.oldScore))))&&((this.scoreChange == rhs.scoreChange)||((this.scoreChange!= null)&&this.scoreChange.equals(rhs.scoreChange))))&&((this.newScore == rhs.newScore)||((this.newScore!= null)&&this.newScore.equals(rhs.newScore))));
    }

}
