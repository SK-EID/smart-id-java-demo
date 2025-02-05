package ee.sk.siddemo.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.Signature;
import org.digidoc4j.SignatureBuilder;
import org.digidoc4j.SignatureProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ee.sk.siddemo.exception.FileUploadException;
import ee.sk.siddemo.exception.SidOperationException;
import ee.sk.siddemo.model.SigningResult;
import ee.sk.siddemo.model.UserDocumentNumberRequest;
import ee.sk.siddemo.model.UserRequest;
import ee.sk.smartid.CertificateParser;
import ee.sk.smartid.HashType;
import ee.sk.smartid.exception.useraction.SessionTimeoutException;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.v3.CertificateLevel;
import ee.sk.smartid.v3.SignableData;
import ee.sk.smartid.v3.SignatureResponseMapper;
import ee.sk.smartid.v3.SingatureResponse;
import ee.sk.smartid.v3.SmartIdClient;
import ee.sk.smartid.v3.rest.dao.DynamicLinkInteraction;
import ee.sk.smartid.v3.rest.dao.DynamicLinkSessionResponse;
import ee.sk.smartid.v3.rest.dao.SessionCertificate;
import ee.sk.smartid.v3.rest.dao.SessionStatus;
import jakarta.servlet.http.HttpSession;

@Service
public class SmartIdV3DynamicLinkSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIdV3DynamicLinkSignatureService.class);

    @Value("${app.signed-files-directory}")
    private String signedFilesDirectory;

    private final SmartIdV3NotificationBasedCertificateChoiceService smartIdV3NotificationBasedCertificateChoiceService;
    private final SmartIdV3SessionsStatusService smartIdV3SessionsStatusService;
    private final SmartIdClient smartIdClientV3;

    public SmartIdV3DynamicLinkSignatureService(SmartIdV3NotificationBasedCertificateChoiceService smartIdV3NotificationBasedCertificateChoiceService, SmartIdV3SessionsStatusService smartIdV3SessionsStatusService, SmartIdClient smartIdClientV3) {
        this.smartIdV3NotificationBasedCertificateChoiceService = smartIdV3NotificationBasedCertificateChoiceService;
        this.smartIdV3SessionsStatusService = smartIdV3SessionsStatusService;
        this.smartIdClientV3 = smartIdClientV3;
    }

    public void startSigningWithDocumentNumber(HttpSession session, UserDocumentNumberRequest userDocumentNumberRequest) {
        DataFile uploadedFile = getUploadedDataFile(userDocumentNumberRequest.getFile());

        var configuration = new Configuration(Configuration.Mode.TEST);
        Container container = ContainerBuilder.aContainer()
                .withConfiguration(configuration)
                .withDataFile(uploadedFile)
                .build();

        X509Certificate certificate = getCertificate(session, userDocumentNumberRequest);

        DataToSign dataToSign = SignatureBuilder.aSignature(container)
                .withSigningCertificate(certificate)
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
                .withSignatureProfile(SignatureProfile.LT)
                .buildDataToSign();

        var signableData = new SignableData(dataToSign.getDataToSign());
        signableData.setHashType(HashType.SHA256);

        DynamicLinkSessionResponse sessionResponse = smartIdClientV3.createDynamicLinkSignature()
                .withCertificateLevel(CertificateLevel.QUALIFIED)
                .withSignableData(signableData)
                .withDocumentNumber(userDocumentNumberRequest.getDocumentNumber())
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN("Sign the document!")))
                .initSignatureSession();
        Instant responseReceivedTime = Instant.now();

        session.setAttribute("sessionSecret", sessionResponse.getSessionSecret());
        session.setAttribute("sessionToken", sessionResponse.getSessionToken());
        session.setAttribute("sessionID", sessionResponse.getSessionID());
        session.setAttribute("responseReceivedTime", responseReceivedTime);
        session.setAttribute("signableData", signableData);
        session.setAttribute("dataToSign", dataToSign);
        session.setAttribute("container", container);

        smartIdV3SessionsStatusService.startPolling(session, sessionResponse.getSessionID());
    }

    public void startSigningWithPersonCode(HttpSession session, UserRequest userRequest) {
        DataFile uploadedFile = getUploadedDataFile(userRequest.getFile());

        var configuration = new Configuration(Configuration.Mode.TEST);
        Container container = ContainerBuilder.aContainer()
                .withConfiguration(configuration)
                .withDataFile(uploadedFile)
                .build();

        X509Certificate certificate = getCertificate(session, userRequest);

        DataToSign dataToSign = SignatureBuilder.aSignature(container)
                .withSigningCertificate(certificate)
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
                .withSignatureProfile(SignatureProfile.LT)
                .buildDataToSign();

        var signableData = new SignableData(dataToSign.getDataToSign());
        signableData.setHashType(HashType.SHA256);

        var semanticsIdentifier = new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, userRequest.getCountry(), userRequest.getNationalIdentityNumber());

        DynamicLinkSessionResponse sessionResponse = smartIdClientV3.createDynamicLinkSignature()
                .withCertificateLevel(CertificateLevel.QUALIFIED)
                .withSignableData(signableData)
                .withSemanticsIdentifier(semanticsIdentifier)
                .withAllowedInteractionsOrder(List.of(DynamicLinkInteraction.displayTextAndPIN("Sign the document!")))
                .initSignatureSession();
        Instant responseReceivedTime = Instant.now();

        session.setAttribute("sessionSecret", sessionResponse.getSessionSecret());
        session.setAttribute("sessionToken", sessionResponse.getSessionToken());
        session.setAttribute("sessionID", sessionResponse.getSessionID());
        session.setAttribute("responseReceivedTime", responseReceivedTime);
        session.setAttribute("signableData", signableData);
        session.setAttribute("dataToSign", dataToSign);
        session.setAttribute("container", container);

        smartIdV3SessionsStatusService.startPolling(session, sessionResponse.getSessionID());
    }

    public boolean checkSignatureStatus(HttpSession session) {
        Optional<SessionStatus> sessionStatus = smartIdV3SessionsStatusService.getSessionsStatus(session.getId());
        return sessionStatus
                .map(status -> {
                    if (status.getState().equals("COMPLETE")) {
                        saveValidateResponse(session, status);
                        session.setAttribute("session_status", "COMPLETED");
                        logger.debug("Mobile device IP address: {}", status.getDeviceIpAddress());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public SigningResult handleSignatureResult(HttpSession session) {
        var dynamicLinkSignatureResponse = (SingatureResponse) session.getAttribute("signing_response");
        if (dynamicLinkSignatureResponse == null) {
            throw new SidOperationException("No signature response found in session");
        }

        byte[] signatureValue = dynamicLinkSignatureResponse.getSignatureValue();
        DataToSign dataToSign = (DataToSign) session.getAttribute("dataToSign");
        Signature signature = dataToSign.finalize(signatureValue);

        Container container = (Container) session.getAttribute("container");
        container.addSignature(signature);
        try {
            File containerFile = File.createTempFile("sid-demo-container-", ".asice");
            Path targetPath = createSavePath(containerFile);
            container.saveAsFile(targetPath.toString());
            return SigningResult.newBuilder()
                    .withResult("Signing successful")
                    .withValid(signature.validateSignature().isValid())
                    .withTimestamp(signature.getTimeStampCreationTime())
                    .withContainerFilePath(targetPath.toString())
                    .build();
        } catch (IOException e) {
            throw new SidOperationException("Could not create container file.", e);
        }
    }

    private DataFile getUploadedDataFile(MultipartFile uploadedFile) {
        try {
            return new DataFile(uploadedFile.getInputStream(), uploadedFile.getOriginalFilename(), uploadedFile.getContentType());
        } catch (IOException e) {
            throw new FileUploadException(e.getCause());
        }
    }

    private X509Certificate getCertificate(HttpSession httpSession, UserDocumentNumberRequest userDocumentNumberRequest) {
        smartIdV3NotificationBasedCertificateChoiceService.startCertificateChoice(httpSession, userDocumentNumberRequest);
        Optional<SessionStatus> certSessionStatus;
        do {
            certSessionStatus = smartIdV3SessionsStatusService.getSessionsStatus(httpSession.getId());
        } while (certSessionStatus.isEmpty());

        SessionCertificate sessionCertificate = certSessionStatus.get().getCert();
        return CertificateParser.parseX509Certificate(sessionCertificate.getValue());
    }

    private X509Certificate getCertificate(HttpSession httpSession, UserRequest userRequest) {
        smartIdV3NotificationBasedCertificateChoiceService.startCertificateChoice(httpSession, userRequest);
        Optional<SessionStatus> certSessionStatus;
        do {
            certSessionStatus = smartIdV3SessionsStatusService.getSessionsStatus(httpSession.getId());
        } while (certSessionStatus.isEmpty());

        SessionCertificate sessionCertificate = certSessionStatus.get().getCert();
        return CertificateParser.parseX509Certificate(sessionCertificate.getValue());
    }

    private Path createSavePath(File containerFile) {
        Path targetDir = Paths.get(signedFilesDirectory);
        File directory = targetDir.toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return targetDir.resolve(containerFile.getName());
    }

    private static void saveValidateResponse(HttpSession session, SessionStatus status) {
        try {
            var dynamicLinkSignatureResponse = SignatureResponseMapper.from(status, CertificateLevel.QUALIFIED.name());
            session.setAttribute("signing_response", dynamicLinkSignatureResponse);
        } catch (SessionTimeoutException ex) {
            throw new SidOperationException(ex.getMessage());
        }
    }
}
