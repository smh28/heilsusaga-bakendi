package utils;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class PasswordGenerator {


    /*
    public static void main( String[] args ) {

        for( int i = 10000; i > 0; i-- ) {
            String password = generateHumanReadablePassword( Words.stored(), 3, "." );
            System.out.println( password + " : " + normalize( password ) );
        }
    }
*/

    /**
     * @return A password
     */
    public static String generateHumanReadablePassword( List<String> sourceWords, int numberOfWords, String separator ) {
        List<String> words = randomObjects( sourceWords, numberOfWords, true );
        return String.join( separator, words );
    }

    /**
     * @return A list of size [numberOfObjects] created by selecting random objects from [sourceList]
     *
     * @param numberOfObjects The number of objects in the resulting list.
     * @param allowDuplicatesInResultingList Indicates if we want duplicates in the resulting list.
     */
    private static <E> List<E> randomObjects( List<E> sourceList, int numberOfObjects, boolean allowDuplicatesInResultingList ) {

        Objects.nonNull( sourceList );

        if( sourceList.size() < numberOfObjects && !allowDuplicatesInResultingList ) {
            throw new IllegalArgumentException( "You can't really get " + numberOfObjects + " random objects from a list containing " + sourceList.size() + " objects without allowing duplicates. Silly." );
        }

        List<E> result = new ArrayList<>();

        while( result.size() < numberOfObjects ) {
            E randomObject = sourceList.get( new Random().nextInt( sourceList.size() ) );

            if( allowDuplicatesInResultingList ) {
                result.add( randomObject );
            }
            else {
                if( !result.contains( randomObject ) ) {
                    result.add( randomObject );
                }
            }
        }

        return result;
    }

    /**
     * @return true If the password matches, ignoring punctuation and whitespace. Returns false if either argument is null.
     */
    public static boolean checkPassword( String enteredPassword, String password ) {
        Objects.nonNull( enteredPassword );
        Objects.nonNull( password );

        String enteredPasswordNormalized = normalize( enteredPassword );
        String passwordNormalized = normalize( password );

        return enteredPasswordNormalized.equals( passwordNormalized );
    }
    /**
     * @return true If the password matches, ignoring punctuation and whitespace. Returns false if either argument is null.
     */
    public static boolean checkPasswordHashed( String enteredPassword, String password ) {
        Objects.nonNull( enteredPassword );
        Objects.nonNull( password );
        String enteredPasswordNormalized = normalize( enteredPassword );
        return BCrypt.checkpw(enteredPasswordNormalized, password);
    }

    /**
     * @return The string with punctuation and whitespace removed.
     */
    private static String normalize( String string ) {
        Objects.nonNull( string );
        string = string.toLowerCase();
        string = string.replace( ".", "" );
        string = string.replace( ",", "" );
        string = string.replace( " ", "" );
        return string;
    }

    public static String hash( String string ) {
        return BCrypt.hashpw(normalize(string), BCrypt.gensalt());
    }
}
