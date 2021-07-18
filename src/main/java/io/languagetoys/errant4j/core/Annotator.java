package io.languagetoys.errant4j.core;

import io.languagetoys.aligner.Aligner;
import io.languagetoys.aligner.Alignment;
import io.languagetoys.aligner.edit.Edit;
import io.languagetoys.errant4j.core.align.TokenAligner;
import io.languagetoys.errant4j.core.classify.Classifier;
import io.languagetoys.errant4j.core.merge.Merger;
import io.languagetoys.spacy4j.api.SpaCy;
import io.languagetoys.spacy4j.api.containers.Doc;
import io.languagetoys.spacy4j.api.containers.Token;

import java.util.List;
import java.util.stream.Collectors;

public interface Annotator {

    static Annotator of(SpaCy spaCy, Merger merger, Classifier classifier) {
        return of(spaCy, new TokenAligner(), merger, classifier);
    }

    static Annotator of(SpaCy spaCy, Aligner<Token> aligner, Merger merger, Classifier classifier) {
        return new AnnotatorImpl(spaCy, aligner, merger, classifier);
    }

    Doc parse(String text);

    /**
     * Run the full pipeline given parsed source and target texts.
     */
    default List<Annotation> annotate(List<Token> source, List<Token> target) {
        Alignment<Token> alignment = align(source, target);
        List<Edit<Token>> merged = merge(alignment.edits());
        return merged.stream()
                .map(edit -> Annotation.of(edit, classify(edit)))
                .collect(Collectors.toList());
    }

    /**
     * Run the 1st step in the pipeline - Align
     */
    Alignment<Token> align(List<Token> source, List<Token> target);

    /**
     * Run the 2nd step in the pipeline - Merge
     */
    List<Edit<Token>> merge(List<Edit<Token>> edits);

    /**
     * Run the 3rd step in the pipeline - Classify
     */
    GrammaticalError classify(Edit<Token> edit);
}
