
package com.codescene.data.ace;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonPropertyOrder({
    "title",
    "level",
    "recommended-action",
    "review-header"
})
@Generated("jsonschema2pojo")
public class Confidence {

    /**
     * Title for presentation
     * (Required)
     * 
     */
    @JsonProperty("title")
    @JsonPropertyDescription("Title for presentation")
    private String title;
    /**
     * Confidence level
     * (Required)
     * 
     */
    @JsonProperty("level")
    @JsonPropertyDescription("Confidence level")
    private Confidence.Level level;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("recommended-action")
    private RecommendedAction recommendedAction;
    /**
     * Header for use when presenting the reason summaries
     * 
     */
    @JsonProperty("review-header")
    @JsonPropertyDescription("Header for use when presenting the reason summaries")
    private String reviewHeader;

    /**
     * Title for presentation
     * (Required)
     * 
     */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     * Confidence level
     * (Required)
     * 
     */
    @JsonProperty("level")
    public Confidence.Level getLevel() {
        return level;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("recommended-action")
    public RecommendedAction getRecommendedAction() {
        return recommendedAction;
    }

    /**
     * Header for use when presenting the reason summaries
     * 
     */
    @JsonProperty("review-header")
    public Optional<String> getReviewHeader() {
        return Optional.ofNullable(reviewHeader);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.recommendedAction == null)? 0 :this.recommendedAction.hashCode()));
        result = ((result* 31)+((this.reviewHeader == null)? 0 :this.reviewHeader.hashCode()));
        result = ((result* 31)+((this.title == null)? 0 :this.title.hashCode()));
        result = ((result* 31)+((this.level == null)? 0 :this.level.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Confidence) == false) {
            return false;
        }
        Confidence rhs = ((Confidence) other);
        return (((((this.recommendedAction == rhs.recommendedAction)||((this.recommendedAction!= null)&&this.recommendedAction.equals(rhs.recommendedAction)))&&((this.reviewHeader == rhs.reviewHeader)||((this.reviewHeader!= null)&&this.reviewHeader.equals(rhs.reviewHeader))))&&((this.title == rhs.title)||((this.title!= null)&&this.title.equals(rhs.title))))&&((this.level == rhs.level)||((this.level!= null)&&this.level.equals(rhs.level))));
    }


    /**
     * Confidence level
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Level {

        _0(0),
        _1(1),
        _2(2),
        _3(3),
        _4(4);
        private final Integer value;
        private final static Map<Integer, Confidence.Level> CONSTANTS = new HashMap<Integer, Confidence.Level>();

        static {
            for (Confidence.Level c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Level(Integer value) {
            this.value = value;
        }

        @JsonValue
        public Integer value() {
            return this.value;
        }

        @JsonCreator
        public static Confidence.Level fromValue(Integer value) {
            Confidence.Level constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException((value +""));
            } else {
                return constant;
            }
        }

    }

}
