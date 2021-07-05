package io.languagetoys.errant4j.lang.en;

import io.languagetoys.aligner.edit.Edit;
import io.languagetoys.errant4j.core.Annotation;
import io.languagetoys.errant4j.core.Errant;
import io.languagetoys.errant4j.core.grammar.GrammaticalError;
import io.languagetoys.errant4j.core.tools.TokenEditUtils;
import io.languagetoys.spacy4j.adapters.corenlp.CoreNLPAdapter;
import io.languagetoys.spacy4j.api.SpaCy;
import io.languagetoys.spacy4j.api.containers.Doc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnnotatorTest {

    private static final Logger logger = LoggerFactory.getLogger(AnnotatorTest.class);
    private Errant errant;

    @BeforeAll
    void setup() {
        this.errant = Errant.en(SpaCy.create(CoreNLPAdapter.create()));
    }

    @Test
    void posTier_Verb() {
        Doc source = nlp("  I   like consume food.");
        Doc target = nlp("I like to eat food.");
        Annotation expected = Edit.builder()
                .substitute("consume")
                .with("to", "eat")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB);
        List<Annotation> actual = annotate(source, target);
        assertEquals(List.of(expected), actual);
    }

    @Test
    void posTier_Part() {
        Doc source = nlp("I want in fly");
        Doc target = nlp("I want to fly");
        Annotation expected = Edit.builder()
                .substitute("in")
                .with("to")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source.tokens(), target.tokens()))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_PARTICLE);
        assertSingleError(expected, source, target);
    }

    @Test
    void posTier_PunctuationEffect() {
        Doc source = nlp("Because");
        Doc target = nlp(", because");
        Annotation expected = Edit.builder()
                .substitute("Because")
                .with(",", "because")
                .atPosition(0, 0)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_PUNCTUATION);
        assertSingleError(expected, source, target);
    }

    @Test
    void tokenTier_Contractions() {
        Doc source = nlp("I've to go home.");
        Doc target = nlp("I have to go home.");
        Annotation expected = Edit.builder()
                .substitute("'ve")
                .with("have")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_CONTRACTION);
        assertSingleError(expected, source, target);
    }

    @Test
    void tokenTier_Orthography() {
        // case
        Doc source1 = nlp("My friend sleeps at Home.");
        Doc target1 = nlp("My friend sleeps at home.");
        Annotation expected1 = Edit.builder()
                .substitute("Home")
                .with("home")
                .atPosition(4, 4)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_ORTHOGRAPHY);
        assertSingleError(expected1, source1, target1);

        // whitespace
        Doc source2 = nlp("My friendsleeps at home.");
        Doc target2 = nlp("My friend sleeps at home.");
        Annotation expected2 = Edit.builder()
                .substitute("friendsleeps")
                .with("friend", "sleeps")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source2, target2))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_ORTHOGRAPHY);
        assertSingleError(expected2, source2, target2);
    }

    @Test
    void tokenTier_Spelling() {
        // case
        Doc source = nlp("My frien sleeps at home.");
        Doc target = nlp("My friend sleeps at home.");
        Annotation expected1 = Edit.builder()
                .substitute("frien")
                .with("friend")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SPELLING);
        assertSingleError(expected1, source, target);
    }

    @Test
    void tokenTier_WordOrder() {
        // case
        Doc source = nlp("This dog is cute");
        Doc target = nlp("This is cute dog");
        Annotation expected1 = Edit.builder()
                .transpose("dog", "is", "cute")
                .to("is", "cute", "dog")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_WORD_ORDER);
        assertSingleError(expected1, source, target);

    }

    @Test
    void morphTier_AdjectiveForm() {
        // case
        Doc source = nlp("This is the most small computer I have ever seen!");
        Doc target = nlp("This is the smallest computer I have ever seen!");
        Annotation expected2 = Edit.builder()
                .substitute("most", "small")
                .with("smallest")
                .atPosition(3, 3)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_ADJECTIVE_FORM);
        assertSingleError(expected2, source, target);
    }

    @Test
    void morphTier_AdjectiveForm2() {
        // case
        Doc source = nlp("This is the big computer.");
        Doc target = nlp("This is the biggest computer.");
        Annotation expected1 = Edit.builder()
                .substitute("big")
                .with("biggest")
                .atPosition(3, 3)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_ADJECTIVE_FORM);
        assertSingleError(expected1, source, target);
    }

    @Test
    void morphTier_nounNumber() {
        Doc source = nlp("Dog are cute.");
        Doc target = nlp("dogs are cute.");
        Annotation expected1 = Edit.builder()
                .substitute("Dog")
                .with("dogs")
                .atPosition(0, 0)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_NOUN_NUMBER);
        assertSingleError(expected1, source, target);
    }

    @Test
    void morphTier_nounPossessive() {
        Doc source1 = nlp("It is at the river edge");
        Doc target1 = nlp("It is at the river's edge");
        Annotation expected1 = Edit.builder()
                .insert("'s")
                .atPosition(5, 5)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.MISSING_NOUN_POSSESSIVE);
        assertSingleError(expected1, source1, target1);

        Doc source2 = nlp("It is at the rivers edge");
        Doc target2 = nlp("It is at the river's edge");
        Annotation expected2 = Edit.builder()
                .substitute("rivers")
                .with("river", "'s")
                .atPosition(4, 4)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source2, target2))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_NOUN_POSSESSIVE);
        assertSingleError(expected2, source2, target2);
    }

    @Disabled("there is a collision between tense and form rules")
    @Test
    public void morphTier_verbFormSubstitutionError() {
        Doc source1 = nlp("Brent would often became stunned");
        Doc target1 = nlp("Brent would often become stunned");
        Annotation expected1 = Edit.builder()
                .substitute("became")
                .with("become")
                .atPosition(3, 3)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_FORM);
        assertSingleError(expected1, source1, target1);

        Doc source2 = nlp("is she go home?");
        Doc target2 = nlp("is she going home?");
        Annotation expected2 = Edit.builder()
                .substitute("go")
                .with("going")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source2, target2))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_FORM);
        assertSingleError(expected2, source2, target2);

        Doc source3 = nlp("is she went home");
        Doc target3 = nlp("is she going home");
        Annotation expected3 = Edit.builder()
                .substitute("went")
                .with("going")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source3, target3))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_FORM);
        assertSingleError(expected3, source3, target3);

        Doc source4 = nlp("is she goes home");
        Doc target4 = nlp("is she going home");
        Annotation expected4 = Edit.builder()
                .substitute("goes")
                .with("going")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source4, target4))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_FORM);
        assertSingleError(expected4, source4, target4);
    }

    @Test
    public void morphTier_verbAgreementSubstitutionError() {
        Doc source1 = nlp("I awaits your response.");
        Doc target1 = nlp("I await your response.");
        Annotation expected1 = Edit.builder()
                .substitute("awaits")
                .with("await")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SUBJECT_VERB_AGREEMENT);
        assertSingleError(expected1, source1, target1);

        Doc source2 = nlp("does she goes home?");
        Doc target2 = nlp("does she go home?");
        Annotation expected2 = Edit.builder()
                .substitute("goes")
                .with("go")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source2, target2))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SUBJECT_VERB_AGREEMENT);
        assertSingleError(expected2, source2, target2);

        Doc source3 = nlp("He must tells him everything.");
        Doc target3 = nlp("He must tell him everything.");
        Annotation expected3 = Edit.builder()
                .substitute("tells")
                .with("tell")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source3, target3))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SUBJECT_VERB_AGREEMENT);
        assertSingleError(expected3, source3, target3);
    }

    @Test
    public void morphTier_verbTenseError() {
        Doc source1 = nlp("I go to see him yesterday.");
        Doc target1 = nlp("I went to see him yesterday.");
        Annotation expected1 = Edit.builder()
                .substitute("go")
                .with("went")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_TENSE);
        assertSingleError(expected1, source1, target1);
    }

    @Test
    public void morphTier_verbFormError_basic() {
        Doc source1 = nlp("I am eat dinner.");
        Doc target1 = nlp("I am eating dinner.");
        Annotation expected1 = Edit.builder()
                .substitute("eat")
                .with("eating")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_FORM);
        assertSingleError(expected1, source1, target1);
    }

    @Test
    public void morphTier_verbFormError_missingInfinitivalTo() {
        Doc source1 = nlp("I would like go home please!");
        Doc target1 = nlp("I would like to go home please!");
        Annotation expected1 = Edit.builder()
                .insert("to")
                .atPosition(3, 3)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.MISSING_VERB_FORM);
        assertSingleError(expected1, source1, target1);
    }

    @Test
    public void morphTier_verbFormError_unnecessaryInfinitivalTo() {
        Doc source1 = nlp("I must to eat now.");
        Doc target1 = nlp("I must eat now.");
        Annotation expected1 = Edit.builder()
                .delete("to")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.UNNECESSARY_VERB_FORM);
        assertSingleError(expected1, source1, target1);
    }

    @Test
    public void morphTier_nounInflection() {
        Doc source1 = nlp("I have five childs.");
        Doc target1 = nlp("I have five children.");
        Annotation expected1 = Edit.builder()
                .substitute("childs")
                .with("children")
                .atPosition(3, 3)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_NOUN_INFLECTION);
        assertSingleError(expected1, source1, target1);
    }

    /**
     * This test fails on spacy-server with REPLACEMENT_VERB, which may actually make sense
     */
    @Test
    public void morphTier_verbInflection() {
        Doc source1 = nlp("I getted the money!");
        Doc target1 = nlp("I got the money!");
        Annotation expected1 = Edit.builder()
                .substitute("getted")
                .with("got")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_VERB_INFLECTION);
        assertSingleError(expected1, source1, target1);
    }

    /**
     * This test fails on spacy-server due to REPLACEMENT_VERB_TENSE
     */
    @Test
    public void morphTier_subjectVerbAgreement() {
        Doc source1 = nlp("I has the money!");
        Doc target1 = nlp("I have the money!");
        Annotation expected1 = Edit.builder()
                .substitute("has")
                .with("have")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SUBJECT_VERB_AGREEMENT);
        assertSingleError(expected1, source1, target1);
    }

    @Test
    public void morphTier_subjectVerbAgreement2() {
        Doc source1 = nlp("Matt like fish.");
        Doc target1 = nlp("Matt likes fish.");
        Annotation expected1 = Edit.builder()
                .substitute("like")
                .with("likes")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SUBJECT_VERB_AGREEMENT);
        assertSingleError(expected1, source1, target1);
    }

    @Test
    void morphTier_subjectVerbAgreement3() {
        Doc source = nlp("If I was you, I would go home.");
        Doc target = nlp("If I were you, I would go home.");
        Annotation expected1 = Edit.builder()
                .substitute("was")
                .with("were")
                .atPosition(2, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source, target))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_SUBJECT_VERB_AGREEMENT);
        assertSingleError(expected1, source, target);
    }

    /**
     * This test fails on spacy-server due to not+always+good not being merged.
     */
    @Test
    void oneWordDoc() {
        Doc source1 = nlp("are");
        Doc target1 = nlp("Students are not always good.");
        Annotation expected1 = Edit.builder()
                .insert("Students")
                .atPosition(0, 0)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.MISSING_NOUN);
        Annotation expected2 = Edit.builder()
                .insert("not", "always", "good")
                .atPosition(1, 2)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.MISSING_OTHER);
        Annotation expected3 = Edit.builder()
                .insert(".")
                .atPosition(1, 5)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.MISSING_PUNCTUATION);
        assertAllErrors(Arrays.asList(expected1, expected2, expected3), source1, target1);
    }

    @Test
    void punctuationOverSpelling() {
        Doc source1 = nlp("Am I early?");
        Doc target1 = nlp("I am not early.");
        Annotation expected1 = Edit.builder()
                .substitute("?")
                .with(".")
                .atPosition(3, 4)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_PUNCTUATION);

        assertContainsError(expected1, source1, target1);
    }

    /**
     * This test fails on spacy-server because wont is split to wo + nt.
     */
    @Test
    void contractionOnMissingApostrophe() {
        Doc source1 = nlp("I wont do that.");
        Doc target1 = nlp("I won't do that.");
        Annotation expected1 = Edit.builder()
                .substitute("wont")
                .with("won't")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_ORTHOGRAPHY);

        assertContainsError(expected1, source1, target1);
    }

    @Test
    void orth() {
        Doc source1 = nlp("they will do no more");
        Doc target1 = nlp("they won't do anymore work");
        Annotation expected1 = Edit.builder()
                .substitute("will")
                .with("won't")
                .atPosition(1, 1)
                .transform(e -> TokenEditUtils.toTokenEdit(e, source1, target1))
                .transform(Annotation::of)
                .withError(GrammaticalError.REPLACEMENT_OTHER);

        assertContainsError(expected1, source1, target1);
    }
    
    void assertSingleError(Annotation expected, Doc source, Doc target) {
        List<Annotation> actual = annotate(source, target);
        assertEquals(List.of(expected), actual);
    }

    void assertContainsError(Annotation expected, Doc source, Doc target) {
        List<Annotation> actual = annotate(source, target);
        if (actual.isEmpty()) {
            throw new AssertionError("Could not matchError expected " + expected + ".\nSource: " + source + "\nTarget: " + target);
        }
        try {
            assertTrue(actual.contains(expected));
        } catch (AssertionError e) {
            logger.info(source.toString());
            logger.info(target.toString());
            throw e;
        }
    }

    void assertAllErrors(List<Annotation> expected, Doc source, Doc target) {
        List<Annotation> actual = annotate(source, target);
        if (actual.isEmpty()) {
            throw new AssertionError("Could not matchError expected " + expected + ".\nSource: " + source + "\nTarget: " + target);
        }
        try {
            assertEquals(expected, actual);
        } catch (AssertionError e) {
            logger.info(source.toString());
            logger.info(target.toString());
            throw e;
        }
    }

    final Doc nlp(String text) {
        return errant.parse(text);
    }

    List<Annotation> annotate(Doc source, Doc target) {
        return errant
                .annotate(source.tokens(), target.tokens())
                .stream()
                .filter(annotation -> !annotation.grammaticalError().isNoneOrIgnored())
                .collect(Collectors.toList());
    }

}
