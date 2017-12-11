package utils;

import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.*;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.schema.impl.XSStringImpl;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Validates SAML 2.0 response from island.is
 * Example usage:
 * 		new SGSIslandIsSaml20Authentication("/some/location/islandIsKeystore.jks", "keystore-password").validateSaml(islandIsToken, ip, userAgent, authId);
 * 		new SGSIslandIsSaml20Authentication(preLoadedIslandIsJksKeystoreObject).validateSaml(islandIsToken, ip, userAgent, null);
 *
 * TODO: útfæra umboðskerfi island.is
 */

public class SGSIslandIsSaml20Authentication {

    public static final String				SAML_ATTRIBUTE_USERSSN				= "UserSSN";// (friendly name „Kennitala“) inniheldur kennitölu notanda sem var að innskrá sig.
    public static final String				SAML_ATTRIBUTE_NAME					= "Name";// (friendly name „Nafn“) inniheldur nafn notanda eins og það kemur fyrir í þjóðskrá eða fyrirtækjaskrá.
    public static final String				SAML_ATTRIBUTE_AUTHENTICATION		= "Authentication"; // (friendly name „Auðkenning“) inniheldur auðkenningaraðferð not- anda. Breytan getur tekið eftirfarandi gildi í AUTHENTICATION_TYPE
    public static final String				SAML_ATTRIBUTE_KEYAUTHENTICATION	= "KeyAuthentication"; // (friendly name „VottunÍslykils“) fylgir með þegar notandi hefur innskráð sig með Íslykli eða styrktum Íslykli. Breytan getur tekið eftirfarandi gildi í
    public static final String				SAML_ATTRIBUTE_IPADDRESS			= "IPAddress"; // (friendly name „IPTala“) inniheldur IP tölu notanda eins og hún birtist við innskráningu. Athugið að notandi getur haft aðra IP tölu gagnvart þjónustuveitanda en innskráningarkerfi svo ekki er hægt að treysta á hún sé sú sama og er í SAML tókanum.
    public static final String				SAML_ATTRIBUTE_USERAGENT			= "UserAgent"; // (friendly name „NotandaStrengur“) inniheldur upplýsingar um vafra notanda sem notaður var við auðkenningu. Dæmi um innihald er „Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0“
    public static final String				SAML_ATTRIBUTE_DESTINATIONSSN		= "DestinationSSN"; // (friendly name „KennitalaMóttakanda“) inniheldur kennitölu þjónustuveitanda.
    public static final String				SAML_ATTRIBUTE_AUTHID				= "AuthID"; // (friendly name „AuðkenningarNúmer“) inniheldur einkvæmt númer sem þjónustuveitandi hefur sent með auðkenningarbeiðni. Ef þjónustuveitandi sendir ekkert þá birtist ekkert númer í svarinu.
    public static final String				SAML_ATTRIBUTE_COMPANYSSN			= "CompanySSN"; // (friendly name „KennitalaLögaðila“) fylgir með þegar notandi hefur innskráð sig með rafrænum starfsmannaskilríkjum eða styrktum rafrænum starfsmanna- skilríkjum. Kennitala lögaðila er fengin úr starfsmannaskilríki notanda.
    public static final String				SAML_ATTRIBUTE_COMPANYNAME			= "CompanyName"; // (friendly name „NafnLögaðila“) fylgir með þegar notandi hefur innskráð sig með rafrænum starfsmannaskilríkjum eða styrktum rafrænum starfsmanna- skilríkjum. Nafn lögaðila er fengin úr starfsmannaskilríki notanda.
    public static final String				SAML_ATTRIBUTE_VALID_FROM			= "SAMLNotBefore"; // SAML is valid from this time
    public static final String				SAML_ATTRIBUTE_VALID_TO				= "SAMLNotOnOrAfter"; // SAML is valid before this time
    public static final SimpleDateFormat DATE_FORMAT_ISO8601					= new SimpleDateFormat( "yyyy.MM.dd'T'hh:mm:ss" );
    public static final String				SAML_MAP_KEY						= "saml20";

    private static final Logger logger								= LoggerFactory.getLogger( SGSIslandIsSaml20Authentication.class );
    private static final String				VALID_SUBJECT_DN					= "SERIALNUMBER=6503760649"; // Þjóðskrá Íslands persidno
    private static final String				VALID_ISSUER_DN_START_VALUE			= "CN=Traustur bunadur";

    private static KeyStore _keyStore;
    private static String					_keyStorePath;
    private static String					_keyStorePassword;

    public enum KEY_AUTHENTICATION_TYPE {
        RAFRAEN_SKILRIKI( "Rafræn skilríki", "notandi var innskráður með rafrænum skilríkjum þegar Íslykill var búinn til" ),
        BREF_I_POSTI( "Bréf í pósti", "notandi innskráði sig með Íslykli sendum á lögheimili einstaklings þegar Íslykill var búinn til" ),
        SKJAL_I_HEIMABANKA( "Skjal í heimabanka", "notandi innskráði sig með Íslykli sendum í heimabankabirtingu einstaklings þegar Íslykill var búinn til" ),
        AFHENT_HJA_THJODSKRA( "Afhent hjá Þjóðskrá gegn framvísun löggildra skilríkja", "" ),
        AFHENT_HJA_SAMSTARFSADILA_THJODSKRA( "Afhent hjá samstarfsaðila Þjóðskrár gegn framvísun löggildra skilríkja", "" ),
        BREF_I_POSTI_TIL_SENDIRADS( "Bréf í pósti á sendiráð og afhending gegng framvísun löggildra skilríkja", "" ),
        OTHEKKT( "Óþekkt", "þegar upplýsingar um það hvað af ofangreindu var notað við gerð Íslykils liggja ekki fyrir" );

        private String	_key;
        private String	_expl;

        KEY_AUTHENTICATION_TYPE( String key, String expl ) {
            this._key = key;
            this._expl = expl;
        }

        public static KEY_AUTHENTICATION_TYPE find( String key ) {
            for ( KEY_AUTHENTICATION_TYPE item : KEY_AUTHENTICATION_TYPE.values() ) {
                if ( item.key().equals( key ) ) {
                    return item;
                }
            }
            return null;
        }

        public String key() {
            return this._key;
        }

        public String explanation() {
            return this._expl;
        }
    }

    public enum AUTHENTICATION_TYPE {
        RAFRAEN_SKILRIKI( "Rafræn skilríki", "þegar notandi hefur innskráð sig með rafrænum skilríkjum" ),
        RAFRAEN_STARFSMANNASKILRIKI( "Rafræn starfsmannaskilríki", "þegar notandi hefur innskráð sig með rafrænum starfsmannaskilríkjum" ),
        ISLYKILL( "Íslykill", "þegar notandi hefur innskráð sig með Íslykli" ),
        STYRKTUR_ISLYKILL( "Styrktur Íslykill", "þegar notandi hefur innskráð sig með Íslykli og styrkingarkóða sendum í síma eða á netfang." ),
        STYRKT_RAFRAEN_SKILRIKI( "Styrkt rafræn skilríki", "þegar notandi hefur innskráð sig með rafrænum skilríkjum og styrkingarkóða sendum í síma eða á netfang." ),
        STYRKT_RAFRAEN_STARFSMANNASKILRIKI( "Styrkt rafræn starfsmannaskilríki", "þegar notandi hefur innskráð sig með rafrænum starfsmannaskilríkjum og styrkingarkóða sendum í síma eða netfang." ),
        OTHEKKT( "Óþekkt", "þegar innskráning er ekki þekkt (ekki notað – villa)" );

        private String	_key;
        private String	_expl;

        AUTHENTICATION_TYPE( String key, String expl ) {
            this._key = key;
            this._expl = expl;
        }

        public static AUTHENTICATION_TYPE find( String key ) {
            for ( AUTHENTICATION_TYPE item : AUTHENTICATION_TYPE.values() ) {
                if ( item.key().equals( key ) ) {
                    return item;
                }
            }
            return null;
        }

        public String key() {
            return this._key;
        }

        public String explanation() {
            return this._expl;
        }
    }

    private KeyStore keyStore() {
        if ( (_keyStore == null) && (_keyStorePath != null) ) {
            try {
                loadKeystore();
            }
            catch ( CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException e ) {
                logger.error( "Error loading keystore", e );
            }
        }
        return _keyStore;
    }

    private void loadKeystore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if ( (_keyStore == null) && (_keyStorePath != null) ) {
            InputStream fis = new FileInputStream( _keyStorePath );
            _keyStore = KeyStore.getInstance( "JKS" );
            _keyStore.load( fis, _keyStorePassword.toCharArray() );
        }
    }

    /**
     * Takes in loaded keystore and performs initialization
     * @param jksKeyStore keystore to validate against
     */
    public SGSIslandIsSaml20Authentication( KeyStore jksKeyStore ) throws ConfigurationException {
        _keyStore = jksKeyStore;
        DefaultBootstrap.bootstrap();
    }

    /**
     * Loads keystore from path and performs initialization
     * @param jksKeyStorePath
     * @param password
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public SGSIslandIsSaml20Authentication( String jksKeyStorePath, String password ) throws ConfigurationException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        _keyStorePath = jksKeyStorePath;
        _keyStorePassword = password;
        loadKeystore();
        DefaultBootstrap.bootstrap();
    }

    /**
     * validates a base64 encoded saml 2.0 message sent from island.is
     * @param samlString base64 encoded saml 2.0
     * @param userIP
     * @param userAgent
     * @param authId
     * @return if validation succeds a hash map with all attributes from the saml 2.0 message is returned else an exception is thrown
     * @throws Exception if validation fails
     */
    public Map<String, String> validateSaml(final String samlString, final String userIP, final String userAgent, final String authId ) throws Exception {
        Map<String, String> attributeMap = null;
        SignableSAMLObject signedObject = (SignableSAMLObject)this.unmarshall( samlString );
        if ( signedObject != null ) {
            SignableSAMLObject samlObject = (SignableSAMLObject)this.validateSignature( signedObject, keyStore() );
            if ( samlObject != null ) {
                Assertion assertion = this.getAssertion( (Response)samlObject, userIP, false );
                if ( assertion != null ) {
                    final DateTime serverDate = new DateTime();
                    if ( assertion.getConditions().getNotBefore().isAfter( serverDate ) ) {
                        throw new Exception( "Token date valid yet (getNotBefore = " + assertion.getConditions().getNotBefore() + " ), server_date: " + serverDate );
                    }
                    if ( assertion.getConditions().getNotOnOrAfter().isBefore( serverDate ) ) {
                        throw new Exception( "Token date expired (getNotOnOrAfter = " + assertion.getConditions().getNotOnOrAfter() + " ), server_date: " + serverDate );
                    }
                    // Validate the assertions for IP, useragent and authId.
                    attributeMap = fetchAssertionAttributes( assertion );
                    attributeMap.put( SAML_ATTRIBUTE_VALID_FROM, DATE_FORMAT_ISO8601.format( assertion.getConditions().getNotBefore().toDate() ) );
                    attributeMap.put( SAML_ATTRIBUTE_VALID_TO, DATE_FORMAT_ISO8601.format( assertion.getConditions().getNotOnOrAfter().toDate() ) );
                    attributeMap.put( SAML_MAP_KEY, samlString );
                    validateAssertion( attributeMap, userIP, userAgent, authId );
                }
            }
        }

        return attributeMap;
    }

    //Unmarshall SAML string
    private final XMLObject unmarshall( final String samlString ) throws Exception {
        try {
            byte[] samlToken = Base64.getDecoder().decode( samlString );
            final BasicParserPool ppMgr = new BasicParserPool();
            final HashMap<String, Boolean> features = new HashMap<String, Boolean>();
            features.put( XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE );
            ppMgr.setBuilderFeatures( features );
            ppMgr.setNamespaceAware( true );
            Document document = ppMgr.parse( new ByteArrayInputStream( samlToken ) );
            if ( document != null ) {
                final Element root = document.getDocumentElement();
                final UnmarshallerFactory unmarshallerFact = Configuration.getUnmarshallerFactory();
                if ( unmarshallerFact != null && root != null ) {
                    final Unmarshaller unmarshaller = unmarshallerFact.getUnmarshaller( root );
                    try {
                        return unmarshaller.unmarshall( root );
                    }
                    catch ( NullPointerException e ) {
                        throw new Exception( "NullPointerException", e );
                    }
                }
                else {
                    throw new Exception( "NullPointerException : unmarshallerFact or root is null" );
                }
            }
            else {
                throw new Exception( "NullPointerException : document is null" );
            }
        }
        catch ( XMLParserException e ) {
            throw new Exception( e );
        }
        catch ( UnmarshallingException e ) {
            throw new Exception( e );
        }
        catch ( NullPointerException e ) {
            throw new Exception( e );
        }
    }

    // validate the saml signature agains all certs int the keystore
    private final SAMLObject validateSignature( final SignableSAMLObject tokenSaml, KeyStore keyStore ) throws Exception {
        try {
            // Validate structure signature
            final SAMLSignatureProfileValidator sigProfValidator = new SAMLSignatureProfileValidator();
            try {
                // Indicates signature id conform to SAML Signature profile
                sigProfValidator.validate( tokenSaml.getSignature() );
            }
            catch ( ValidationException e ) {
                //ValidationException: signature isn't conform to SAML Signature profile.
                throw new Exception( e );
            }
            String aliasCert = null;
            X509Certificate certificate;

            final KeyInfo keyInfo = tokenSaml.getSignature().getKeyInfo();
            final org.opensaml.xml.signature.X509Certificate xmlCert = keyInfo.getX509Datas().get( 0 ).getX509Certificates().get( 0 );
            final CertificateFactory certFact = CertificateFactory.getInstance( "X.509" );
            final ByteArrayInputStream bis = new ByteArrayInputStream( Base64.getDecoder().decode( xmlCert.getValue() ) );
            final X509Certificate cert = (X509Certificate)certFact.generateCertificate( bis );

            // Exist only one certificate
            final BasicX509Credential entityX509Cred = new BasicX509Credential();
            entityX509Cred.setEntityCertificate( cert );

            try {
                cert.checkValidity();
            }
            catch ( CertificateExpiredException exp ) {
                throw new Exception( "Certificate expired." );
            }
            boolean trusted = false;
            for (final Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
                aliasCert = e.nextElement();
                certificate = (X509Certificate)keyStore.getCertificate( aliasCert );
                try {
                    cert.verify( certificate.getPublicKey() );
                    trusted = true;
                    break;
                }
                catch ( Exception ex ) {
                    //Do nothing - cert not trusted yet so we try the next one
                }
            }
            if ( !trusted )
                throw new Exception( "Certificate is not trusted." );
            else {
                if ( cert.getSubjectDN().toString().contains( VALID_SUBJECT_DN ) && cert.getIssuerDN().toString().startsWith( VALID_ISSUER_DN_START_VALUE ) )
                    trusted = true;
                else {
                    throw new Exception( "Certificate is not trusted." );
                }
            }
            // Validate signature
            final SignatureValidator sigValidator = new SignatureValidator( entityX509Cred );
            sigValidator.validate( tokenSaml.getSignature() );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        return tokenSaml;
    }

    private Assertion getAssertion( final Response samlResponse, final String userIP, final boolean ipValidate ) throws Exception {
        if ( samlResponse.getAssertions() == null || samlResponse.getAssertions().isEmpty() ) {
            //Assertion is null or empty
            return null;
        }
        final Assertion assertion = (Assertion)samlResponse.getAssertions().get( 0 );
        for (final Iterator<SubjectConfirmation> iter = assertion.getSubject().getSubjectConfirmations().iterator(); iter.hasNext(); ) {
            final SubjectConfirmation element = iter.next();
            final boolean isBearer = SubjectConfirmation.METHOD_BEARER.equals( element.getMethod() );
            if ( ipValidate ) {
                if ( isBearer ) {
                    if ( isBlank( userIP ) ) {
                        throw new Exception( "browser_ip is null or empty." );
                    }
                    else if ( isBlank( element.getSubjectConfirmationData().getAddress() ) ) {
                        throw new Exception( "token_ip attribute is null or empty." );
                    }
                }
                final boolean ipEqual = element.getSubjectConfirmationData().getAddress().equals( userIP );
                // Validation ipUser
                if ( !ipEqual && ipValidate ) {
                    throw new Exception( "IPs doesn't match : token_ip (" + element.getSubjectConfirmationData().getAddress() + ") browser_ip (" + userIP + ")" );
                }
            }
        }
        return assertion;
    }

    private boolean isBlank( String str ) {
        return ((str == null) || str.trim().length() < 1);
    }

    /**
     * Validate that attributes are precent and returns them in a hashmap
     * @param assertion The assertion to parse
     * @throws Exception if no attributes are found
     */
    private Map<String, String> fetchAssertionAttributes( final Assertion assertion ) throws Exception {
        final List<XMLObject> listExtensions = assertion.getOrderedChildren();
        boolean find = false;
        AttributeStatement requestedAttr = null;
        // Search the attribute statement.
        for ( int i = 0; i < listExtensions.size() && !find; i++ ) {
            final XMLObject xml = listExtensions.get( i );
            if ( xml instanceof AttributeStatement ) {
                requestedAttr = (AttributeStatement)xml;
                find = true;
            }
        }
        if ( !find ) {
            throw new Exception( "AttributeStatement it's not present." );
        }
        final List<Attribute> reqAttrs = requestedAttr.getAttributes();
        String attributeName, tempValue;
        XMLObject xmlObj;
        // Process the attributes.
        Map<String, String> attributeMap = new HashMap<String, String>();
        for ( int nextAttribute = 0; nextAttribute < reqAttrs.size(); nextAttribute++ ) {
            final Attribute attribute = reqAttrs.get( nextAttribute );
            attributeName = attribute.getName();
            xmlObj = attribute.getOrderedChildren().get( 0 );
            tempValue = ((XSStringImpl)xmlObj).getValue();

            attributeMap.put( attributeName, tempValue );
        }
        return attributeMap;
    }

    /**
     * Validate assertions for IP, user agent and auth ID
     * @param ip The user IP
     * @param ua The users user agent
     * @param authId The auth ID, if null then this attribute is not validated
     * @throws Exception
     */
    private void validateAssertion( final Map<String, String> attributeMap, String ip, String ua, String authId ) throws Exception {
        boolean ipOk = false, uaOk = false, authIdOk = false;
        for ( Map.Entry<String, String> entry : attributeMap.entrySet() ) {
            if ( SAML_ATTRIBUTE_IPADDRESS.equals( entry.getKey() ) ) {
                logger.debug( SAML_ATTRIBUTE_IPADDRESS + ": " + entry.getValue() );
                ipOk = (entry.getValue() != null) && entry.getValue().equals( ip );
            }
            if ( SAML_ATTRIBUTE_USERAGENT.equals( entry.getKey() ) ) {
                uaOk = (entry.getValue() != null) && entry.getValue().equals( ua );
                logger.debug( SAML_ATTRIBUTE_USERAGENT + ": " + entry.getValue() );
            }
            if ( SAML_ATTRIBUTE_AUTHID.equals( entry.getKey() ) ) {
                // does not validate authId if its not provided by host
                authIdOk = (authId == null) || (entry.getValue() != null) && entry.getValue().equals( authIdOk );
                logger.debug( SAML_ATTRIBUTE_AUTHID + ": " + entry.getValue() );
            }
        }
        if ( ipOk || authIdOk || uaOk )
            logger.debug( "Assertion valid." );
        else
            throw new Exception( String.format( "Assertions not valid. IP valid %b, user agent valid %b, auth ID valid %b", ipOk, uaOk, authIdOk ) );
    }
}
