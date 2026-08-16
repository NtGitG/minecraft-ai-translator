package fr.ntgitg.mineglot.core.update;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^[vV]?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");
    private static final Pattern NUMERIC_IDENTIFIER = Pattern.compile("0|[1-9]\\d*");
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

    private final BigInteger major;
    private final BigInteger minor;
    private final BigInteger patch;
    private final List<String> preReleaseIdentifiers;

    private SemanticVersion(BigInteger major, BigInteger minor, BigInteger patch,
                            List<String> preReleaseIdentifiers) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preReleaseIdentifiers = preReleaseIdentifiers;
    }

    static SemanticVersion parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Version is null");
        }

        Matcher matcher = VERSION_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: " + value);
        }

        List<String> preRelease = new ArrayList<>();
        if (matcher.group(4) != null) {
            Collections.addAll(preRelease, matcher.group(4).split("\\."));
            for (String identifier : preRelease) {
                if (DIGITS_ONLY.matcher(identifier).matches()
                        && !NUMERIC_IDENTIFIER.matcher(identifier).matches()) {
                    throw new IllegalArgumentException(
                            "Numeric pre-release identifiers cannot have leading zeroes: "
                                    + value);
                }
            }
        }

        return new SemanticVersion(
                new BigInteger(matcher.group(1)),
                new BigInteger(matcher.group(2)),
                new BigInteger(matcher.group(3)),
                Collections.unmodifiableList(preRelease));
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int result = major.compareTo(other.major);
        if (result != 0) {
            return result;
        }

        result = minor.compareTo(other.minor);
        if (result != 0) {
            return result;
        }

        result = patch.compareTo(other.patch);
        if (result != 0) {
            return result;
        }

        if (preReleaseIdentifiers.isEmpty() && other.preReleaseIdentifiers.isEmpty()) {
            return 0;
        }
        if (preReleaseIdentifiers.isEmpty()) {
            return 1;
        }
        if (other.preReleaseIdentifiers.isEmpty()) {
            return -1;
        }

        int sharedLength = Math.min(preReleaseIdentifiers.size(),
                other.preReleaseIdentifiers.size());
        for (int i = 0; i < sharedLength; i++) {
            result = compareIdentifier(preReleaseIdentifiers.get(i),
                    other.preReleaseIdentifiers.get(i));
            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(preReleaseIdentifiers.size(), other.preReleaseIdentifiers.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = NUMERIC_IDENTIFIER.matcher(left).matches();
        boolean rightNumeric = NUMERIC_IDENTIFIER.matcher(right).matches();

        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }
}
