package de.uni_hamburg.corpora.validation.quest;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing the metadata criteria including property name, lower and upper bounds for cardinality and locator
 * to find the property in a metadata file
 */
public class GenericMetadataCriterion {

    public static class Bounds {
        public enum EBounds {B0, B1, Unbounded, NA}

        EBounds upper;
        EBounds lower;

        public Bounds() {}

        public Bounds(EBounds lower, EBounds upper) {
            this.upper = upper ;
            this.lower = lower ;
        }

        public static String toString(EBounds b) {
            String s ;
            switch (b) {
                case B0:
                    s = "0";
                    break;
                case B1:
                    s = "1";
                    break;
                case Unbounded:
                    s = "unbounded";
                    break;
                default:
                    s = "N/A";
                    break;
            }
            return s ;
        }
    }

    public static class ToBounds extends AbstractBeanField {

        public ToBounds() {}

        @Override
        protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
            return new Bounds(toLower(value),toUpper(value));
        }

        private Bounds.EBounds toLower(String value) {
            if (value.startsWith("0")) {
                return Bounds.EBounds.B0 ;
            }
            else if (value.startsWith("1")) {
                return Bounds.EBounds.B1;
            }
            else {
                return Bounds.EBounds.NA;
            }
        }

        private Bounds.EBounds toUpper(String value) {
            if (value.endsWith("0")) {
                return Bounds.EBounds.B0 ;
            }
            else if (value.endsWith("1")) {
                return Bounds.EBounds.B1 ;
            }
            else if (value.endsWith("unbounded")) {
                return Bounds.EBounds.Unbounded ;
            }
            else {
                return Bounds.EBounds.NA ;
            }
        }
    }

    public static class ToType extends AbstractBeanField {

        @Override
        protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
            // Split type at " OR " and use optional to represent known types or an empty value otherwise
            return Arrays.stream(value.split("\\s+OR\\s+")).map(this::toOptional).collect(Collectors.toList()) ;
        }

        private Optional<String> toOptional(String value) {
            if (value.equalsIgnoreCase("string") || value.equalsIgnoreCase("uri") ||
                    value.equalsIgnoreCase("date"))
                return Optional.of(value) ;
            else
                return Optional.empty() ;
        }
    }

    public static int compareToBounds(int i, Bounds.EBounds b) {
        int j = 0 ; // same as Bounds.B0
        // Update j depending on b
        if (b == Bounds.EBounds.B1)
            j = 1 ;
        // Unbounded is always larger than any number
        else if (b == Bounds.EBounds.Unbounded)
            j = Integer.MAX_VALUE ;
        // Everything else is smaller
        else if (b == Bounds.EBounds.NA)
            return Integer.MIN_VALUE ;
        // Use the built-in comparison
        return Integer.compare(i,j);

    }

    public GenericMetadataCriterion() {}
    // First column is the name of the property
    @CsvBindByPosition(position = 0)
    String name ;
    // Second column is the lower and upper bounds of cardinality
    // Custom parser is defined in ToBound
    @CsvCustomBindByPosition(position = 1, converter = ToBounds.class)
    Bounds bounds ;
    // Third column is the lower and upper bounds of cardinality
    // Custom parser is defined in ToType
    @CsvCustomBindByPosition(position = 2, converter = ToType.class)
    List<Optional<String>> type ;
    // Fourth column are potential locators, just split on " OR "
    @CsvBindAndSplitByPosition(position = 3, elementType = String.class, splitOn = "\\s+OR\\s+")
    List<String> locator;
}

