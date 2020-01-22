package com.alex.ach;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.afrunt.jach.ACH;
import com.afrunt.jach.document.ACHBatch;
import com.afrunt.jach.document.ACHBatchDetail;
import com.afrunt.jach.document.ACHDocument;
import com.afrunt.jach.domain.BatchControl;
import com.afrunt.jach.domain.FileControl;
import com.afrunt.jach.domain.FileHeader;
import com.afrunt.jach.domain.GeneralBatchHeader;
import com.afrunt.jach.domain.detail.WEBEntryDetail;

@SpringBootApplication
public class AlexAchApplication {

    private static final String COMPANY_IDENTIFICATION = "9987654321";
    
    private static final String ORIGINATOR_DFI_IDENTIFIER = "12345678";
    
    private static final String ROUTING_NO = "999999999";

    private static final String ACCOUNT_NO = "1212121458";
    
    private static final String INDIVIDUAL_ID = "PH4ID68N9N0C";
    
    private static final int TRANSACTION_TYPE = TransactionType.CREDIT_TO_SAVINGS_PRENOTE.getCode();
    
//    private static final BigDecimal AMOUNT = new BigDecimal(0);
    

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final String CCD_ENTRY_CHECK_DIGIT = "2";

    private static final String COMPANY_ENTRY_DESCRIPTION_OFFSET = "OFFSET";

    private static final String STANDARD_ENTRY_CLASS_CODE_CCD = "CCD";

    private static final String ADDENDA_RECORD_INDICATOR = "0";

    private static final String PAYMENT_TYPE_CODE_S = "S";

    
    
    

    private static final String ACCOUNT_TYPE_SAVINGS = "savings";

    private static final String STANDARD_ENTRY_CLASS_CODE_WEB = "WEB";

    private static final String COMPANY_ENTRY_DESCRIPTION_WEBREFUND = "WEBREFUND";

    private static final String STANDARD_ENTRY_CLASS_CODE_PPD = "PPD";

    private static final String SERVICE_CLASS_CODE_DEBIT = "225";

    private static final String SERVICE_CLASS_CODE_CREDIT = "220";

    private static final int SERVICE_CLASS_CODE_DEBIT_NUMERIC = 225;

    private static final int SERVICE_CLASS_CODE_CREDIT_NUMERIC = 220;

    private static final String FILE_HEADER_IMMEDIATE_ORIGIN_NAME = "CCBill";

    private static final String FILE_HEADER_IMMEDIATE_DESTINATION_NAME = "Merrick Bank";

    private static final String FILE_HEADER_FORMAT_CODE = "1";

    private static final String FILE_HEADER_BLOCKING_FACTOR = "10";

    private static final String FILE_HEADER_PRIORITY_CODE = "01";

    private static final String FILE_NAME_PREFIX = "CCBILL_";

    private static final String FILE_NAME_DATE_FORMAT = "yyyyMMdd";

    private static final String ACH_FILE_EXTENSION = ".ACH";

    private static final String SUM_FILE_EXTENSION = ".SUM";

    private static String fileHeaderImmediateDestination = "777777777";

    private static String fileHeaderImmediateOrigin = "999999999";

    private static String fileModifier = "0";

    public static void main(String[] args) {
//		SpringApplication.run(AlexAchApplication.class, args);

        ACHDocument document = new ACHDocument();

        document.setFileHeader(createFileHeader(Calendar.getInstance()));
        document.setBatches(createBatches());
        document.setFileControl(createFileControl(document));

        ACH ach = new ACH();
        System.out.println(ach.write(document));
    }
    
    private static FileHeader createFileHeader(Calendar calendar) {
        FileHeader fileHeader = new FileHeader();
        fileHeader.setPriorityCode(FILE_HEADER_PRIORITY_CODE);
        fileHeader.setImmediateDestination(fileHeaderImmediateDestination);
        fileHeader.setImmediateOrigin(fileHeaderImmediateOrigin);

        Date date = calendar.getTime();
        fileHeader.setFileCreationDate(date);
        fileHeader.setFileCreationTime(getFileCreationTime(calendar));

        fileHeader.setFileIdModifier(fileModifier);

        fileHeader.setBlockingFactor(FILE_HEADER_BLOCKING_FACTOR);
        fileHeader.setFormatCode(FILE_HEADER_FORMAT_CODE);
        fileHeader.setImmediateDestinationName(FILE_HEADER_IMMEDIATE_DESTINATION_NAME);
        fileHeader.setImmediateOriginName(FILE_HEADER_IMMEDIATE_ORIGIN_NAME);
        return fileHeader;
    }
    
    private static List<ACHBatch> createBatches() {
        List<ACHBatch> webAchs = new ArrayList<>();

        ACHBatch achBatch = new ACHBatch();
        achBatch.setBatchHeader(createBatchHeader());

        ACHBatchDetail achBatchDetail = new ACHBatchDetail();
        achBatchDetail.setDetailRecord(createWEBEntryDetail());
        achBatch.addDetail(achBatchDetail);
        achBatch.setBatchControl(createBatchControl(webAchs));

        webAchs.add(achBatch);

        return webAchs;
    }

    private static FileControl createFileControl(ACHDocument document) {
        int entriesCount = getEntriesCount(document);

        long entryHash = getEntryHash(document);

        double totalDebits = getTotalDebits(document);

        double totalCredits = getTotalCredits(document);

        int roundedUpBlockCount = calculateBlockCount(document);

        FileControl fileControl = new FileControl();
        fileControl.setBatchCount(document.getBatchCount());

        fileControl.setBlockCount(roundedUpBlockCount);
        fileControl.setEntryAddendaCount(entriesCount);
        fileControl.setEntryHashTotals(entryHash);
        fileControl.setTotalDebits(BigDecimal.valueOf(totalDebits));
        fileControl.setTotalCredits(BigDecimal.valueOf(totalCredits));

        return fileControl;
    }

    private static int getEntriesCount(ACHDocument document) {
        return document.getBatches().stream()
                .map(ACHBatch::getDetails)
                .mapToInt(List::size)
                .sum();
    }

    private static long getEntryHash(ACHDocument document) {
        return Long.parseLong(
                StringUtils.substring(
                        String.valueOf(document.getBatches().stream().mapToLong(achBatch -> achBatch.getBatchControl().getEntryHash().longValue()).sum()), -10));
    }

    private static double getTotalDebits(ACHDocument document) {
        BigDecimal sum = new BigDecimal(0);
        for (ACHBatch batch : document.getBatches()) {
            sum = sum.add(batch.getBatchControl().getTotalDebits());
        }
        return sum.doubleValue();
    }

    private static double getTotalCredits(ACHDocument document) {
        BigDecimal sum = new BigDecimal(0);
        for (ACHBatch batch : document.getBatches()) {
            sum = sum.add(batch.getBatchControl().getTotalCredits());
        }
        return sum.doubleValue();
    }

    private static int calculateBlockCount(ACHDocument document) {
        int numberOfLines = getNumberOfLines(document);

        double blockCount = ((double) numberOfLines) / 10;

        return (int) Math.ceil(blockCount);
    }

    private static int getNumberOfLines(ACHDocument document) {

        int entriesCount = getEntriesCount(document);

        // 2 -> one line for file header and one for control record
        // (document.getBatchCount() * 2) -> each batch has header and control records, so we are
        // multiplying each batch with 2
        // entriesCount -> number of actual entries
        return 2 + document.getBatchCount() * 2 + entriesCount;
    }

    private static GeneralBatchHeader createBatchHeader() {
        GeneralBatchHeader batchHeader = new GeneralBatchHeader();
        batchHeader.setServiceClassCode(SERVICE_CLASS_CODE_CREDIT);
        batchHeader.setStandardEntryClassCode(STANDARD_ENTRY_CLASS_CODE_PPD);
        batchHeader.setCompanyEntryDescription(COMPANY_ENTRY_DESCRIPTION_WEBREFUND);
        batchHeader.setCompanyName("CCBill");
        batchHeader.setCompanyID(COMPANY_IDENTIFICATION);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date date = cal.getTime();

        batchHeader.setEffectiveEntryDate(date);
        batchHeader.setOriginatorStatusCode(FILE_HEADER_FORMAT_CODE);
        batchHeader.setOriginatorDFIIdentifier(ORIGINATOR_DFI_IDENTIFIER);
        batchHeader.setBatchNumber(1);

        return batchHeader;
    }

    private static WEBEntryDetail createWEBEntryDetail() {

        WEBEntryDetail webEntryDetail = new WEBEntryDetail();
        webEntryDetail.setTransactionCode(TRANSACTION_TYPE);
        webEntryDetail.setReceivingDfiIdentification(StringUtils.substring(ROUTING_NO, 0, 8));
        webEntryDetail.setCheckDigit(Short.valueOf(StringUtils.substring(ROUTING_NO, -1)));
        webEntryDetail.setDfiAccountNumber(ACCOUNT_NO);
        webEntryDetail.setAmount(new BigDecimal(0));
        webEntryDetail.setIdentificationNumber(INDIVIDUAL_ID);
        webEntryDetail.setIndividualName(StringUtils.substring("Alex Stoy", 0, 22));
        webEntryDetail.setPaymentTypeCode(PAYMENT_TYPE_CODE_S);
        webEntryDetail.setAddendaRecordIndicator(Short.valueOf(ADDENDA_RECORD_INDICATOR));
        webEntryDetail.setTraceNumber(123456l);

        return webEntryDetail;
    }

    private static BatchControl createBatchControl(List<ACHBatch> webAchs) {

        BatchControl batchControl = new BatchControl();

        final long entryHash = webAchs.stream().mapToLong(webAch -> Integer.valueOf(StringUtils.substring("067014026", 0, 8))).sum();
        batchControl.setEntryHash(new BigInteger(StringUtils.substring(String.valueOf(entryHash), -10)));
        
        batchControl.setServiceClassCode(SERVICE_CLASS_CODE_CREDIT_NUMERIC);
        batchControl.setEntryAddendaCount(webAchs.size());
        batchControl.setTotalDebits(new BigDecimal(0));
        batchControl.setTotalCredits(new BigDecimal(0));
        batchControl.setCompanyIdentification(COMPANY_IDENTIFICATION);
        batchControl.setOriginatingDfiIdentification(ORIGINATOR_DFI_IDENTIFIER);
        batchControl.setBatchNumber(1);

        return batchControl;
    }

    private static String getFileCreationTime(Calendar calendar) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2, "0"));
        builder.append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MINUTE)), 2, "0"));
        return builder.toString();
    }
}
