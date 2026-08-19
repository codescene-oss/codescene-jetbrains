
package com.codescene.data.review;

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
    "score",
    "file-level-code-smells",
    "function-level-code-smells",
    "raw-score"
})
@Generated("jsonschema2pojo")
public class Review {

    /**
     * If file is scorable, this will be a number between 1.0 and 10.0
     * 
     */
    @JsonProperty("score")
    @JsonPropertyDescription("If file is scorable, this will be a number between 1.0 and 10.0")
    private Double score;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-level-code-smells")
    private List<CodeSmell> fileLevelCodeSmells = new ArrayList<CodeSmell>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("function-level-code-smells")
    private List<Function> functionLevelCodeSmells = new ArrayList<Function>();
    /**
     * Base64 encoded review data used by the delta analysis.
     * (Required)
     * 
     */
    @JsonProperty("raw-score")
    @JsonPropertyDescription("Base64 encoded review data used by the delta analysis.")
    private String rawScore;
    @JsonProperty("git-blob-sha")
    private String gitBlobSha;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Review() {
    }

    /**
     * 
     * @param score
     *     If file is scorable, this will be a number between 1.0 and 10.0.
     * @param rawScore
     *     Base64 encoded review data used by the delta analysis.
     */
    public Review(Double score, List<CodeSmell> fileLevelCodeSmells, List<Function> functionLevelCodeSmells, String rawScore) {
        super();
        this.score = score;
        this.fileLevelCodeSmells = fileLevelCodeSmells;
        this.functionLevelCodeSmells = functionLevelCodeSmells;
        this.rawScore = rawScore;
    }

    /**
     * If file is scorable, this will be a number between 1.0 and 10.0
     * 
     */
    @JsonProperty("score")
    public Optional<Double> getScore() {
        return Optional.ofNullable(score);
    }

    /**
     * If file is scorable, this will be a number between 1.0 and 10.0
     * 
     */
    @JsonProperty("score")
    public void setScore(Double score) {
        this.score = score;
    }

    public Review withScore(Double score) {
        this.score = score;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-level-code-smells")
    public List<CodeSmell> getFileLevelCodeSmells() {
        return fileLevelCodeSmells;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("file-level-code-smells")
    public void setFileLevelCodeSmells(List<CodeSmell> fileLevelCodeSmells) {
        this.fileLevelCodeSmells = fileLevelCodeSmells;
    }

    public Review withFileLevelCodeSmells(List<CodeSmell> fileLevelCodeSmells) {
        this.fileLevelCodeSmells = fileLevelCodeSmells;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("function-level-code-smells")
    public List<Function> getFunctionLevelCodeSmells() {
        return functionLevelCodeSmells;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("function-level-code-smells")
    public void setFunctionLevelCodeSmells(List<Function> functionLevelCodeSmells) {
        this.functionLevelCodeSmells = functionLevelCodeSmells;
    }

    public Review withFunctionLevelCodeSmells(List<Function> functionLevelCodeSmells) {
        this.functionLevelCodeSmells = functionLevelCodeSmells;
        return this;
    }

    /**
     * Base64 encoded review data used by the delta analysis.
     * (Required)
     * 
     */
    @JsonProperty("raw-score")
    public String getRawScore() {
        return rawScore;
    }

    /**
     * Base64 encoded review data used by the delta analysis.
     * (Required)
     * 
     */
    @JsonProperty("raw-score")
    public void setRawScore(String rawScore) {
        this.rawScore = rawScore;
    }

    public Review withRawScore(String rawScore) {
        this.rawScore = rawScore;
        return this;
    }

    @JsonProperty("git-blob-sha")
    public String getGitBlobSha() {
        return gitBlobSha;
    }

    @JsonProperty("git-blob-sha")
    public void setGitBlobSha(String gitBlobSha) {
        this.gitBlobSha = gitBlobSha;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.score == null)? 0 :this.score.hashCode()));
        result = ((result* 31)+((this.rawScore == null)? 0 :this.rawScore.hashCode()));
        result = ((result* 31)+((this.functionLevelCodeSmells == null)? 0 :this.functionLevelCodeSmells.hashCode()));
        result = ((result* 31)+((this.fileLevelCodeSmells == null)? 0 :this.fileLevelCodeSmells.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Review) == false) {
            return false;
        }
        Review rhs = ((Review) other);
        return (((((this.score == rhs.score)||((this.score!= null)&&this.score.equals(rhs.score)))&&((this.rawScore == rhs.rawScore)||((this.rawScore!= null)&&this.rawScore.equals(rhs.rawScore))))&&((this.functionLevelCodeSmells == rhs.functionLevelCodeSmells)||((this.functionLevelCodeSmells!= null)&&this.functionLevelCodeSmells.equals(rhs.functionLevelCodeSmells))))&&((this.fileLevelCodeSmells == rhs.fileLevelCodeSmells)||((this.fileLevelCodeSmells!= null)&&this.fileLevelCodeSmells.equals(rhs.fileLevelCodeSmells))));
    }

}
