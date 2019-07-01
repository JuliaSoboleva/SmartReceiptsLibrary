package co.smartreceipts.android.workers.reports.pdf.pdfbox;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import co.smartreceipts.android.R;
import co.smartreceipts.android.date.DateFormatter;
import co.smartreceipts.android.date.DisplayableDate;
import co.smartreceipts.android.filters.LegacyReceiptFilter;
import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.Distance;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.comparators.ReceiptDateComparator;
import co.smartreceipts.android.model.converters.DistanceToReceiptsConverter;
import co.smartreceipts.android.model.utils.ModelUtils;
import co.smartreceipts.android.persistence.database.controllers.grouping.results.CategoryGroupingResult;
import co.smartreceipts.android.persistence.database.controllers.grouping.results.SumCategoryGroupingResult;
import co.smartreceipts.android.purchases.model.InAppPurchase;
import co.smartreceipts.android.purchases.wallet.PurchaseWallet;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.settings.catalog.UserPreference;
import co.smartreceipts.android.workers.reports.ReportResourcesManager;
import co.smartreceipts.android.workers.reports.pdf.colors.PdfColorStyle;
import co.smartreceipts.android.workers.reports.pdf.fonts.PdfFontStyle;
import co.smartreceipts.android.workers.reports.pdf.renderer.empty.EmptyRenderer;
import co.smartreceipts.android.workers.reports.pdf.renderer.formatting.Alignment;
import co.smartreceipts.android.workers.reports.pdf.renderer.grid.GridRenderer;
import co.smartreceipts.android.workers.reports.pdf.renderer.grid.GridRowRenderer;
import co.smartreceipts.android.workers.reports.pdf.renderer.impl.PdfTableGenerator;
import co.smartreceipts.android.workers.reports.pdf.renderer.text.TextRenderer;

public class PdfBoxReceiptsTablePdfSection extends PdfBoxSection {

    private static final int EMPTY_ROW_HEIGHT_NORMAL = 40;
    private static final int EMPTY_ROW_HEIGHT_SMALL = 10;


    private final List<Receipt> receipts;
    private final List<Column<Receipt>> receiptColumns;

    private final List<Distance> distances;
    private final List<Column<Distance>> distanceColumns;

    private final List<SumCategoryGroupingResult> categories;
    private final List<Column<SumCategoryGroupingResult>> categoryColumns;

    private final List<CategoryGroupingResult> groupingResults;

    private final UserPreferenceManager preferenceManager;
    private final PurchaseWallet purchaseWallet;

    private final ReportResourcesManager reportResourcesManager;

    private PdfBoxWriter writer;

    protected PdfBoxReceiptsTablePdfSection(@NonNull PdfBoxContext context,
                                            @NonNull ReportResourcesManager reportResourcesManager,
                                            @NonNull Trip trip,
                                            @NonNull List<Receipt> receipts,
                                            @NonNull List<Column<Receipt>> receiptColumns,
                                            @NonNull List<Distance> distances,
                                            @NonNull List<Column<Distance>> distanceColumns,
                                            @NonNull List<SumCategoryGroupingResult> categories,
                                            @NonNull List<Column<SumCategoryGroupingResult>> categoryColumns,
                                            @NonNull List<CategoryGroupingResult> groupingResults,
                                            @NonNull PurchaseWallet purchaseWallet) {
        super(context, trip);
        this.receipts = Preconditions.checkNotNull(receipts);
        this.distances = Preconditions.checkNotNull(distances);
        this.categories = Preconditions.checkNotNull(categories);
        this.groupingResults = Preconditions.checkNotNull(groupingResults);
        this.receiptColumns = Preconditions.checkNotNull(receiptColumns);
        this.distanceColumns = Preconditions.checkNotNull(distanceColumns);
        this.categoryColumns = Preconditions.checkNotNull(categoryColumns);
        this.preferenceManager = Preconditions.checkNotNull(context.getPreferences());
        this.purchaseWallet = Preconditions.checkNotNull(purchaseWallet);
        this.reportResourcesManager = Preconditions.checkNotNull(reportResourcesManager);
    }


    @Override
    public void writeSection(@NonNull PDDocument doc, @NonNull PdfBoxWriter writer) throws IOException {

        final DefaultPdfBoxPageDecorations pageDecorations = new DefaultPdfBoxPageDecorations(pdfBoxContext, trip);
        final ReceiptsTotals totals = new ReceiptsTotals(trip, receipts, distances, preferenceManager);

        // switch to landscape mode
        if (preferenceManager.get(UserPreference.ReportOutput.PrintReceiptsTableInLandscape)) {
            pdfBoxContext.setPageSize(new PDRectangle(pdfBoxContext.getPageSize().getHeight(),
                    pdfBoxContext.getPageSize().getWidth()));
        }

        this.writer = writer;
        this.writer.newPage();

        final float availableWidth = pdfBoxContext.getPageSize().getWidth() - 2 * pdfBoxContext.getPageMarginHorizontal();
        final float availableHeight = pdfBoxContext.getPageSize().getHeight() - 2 * pdfBoxContext.getPageMarginVertical()
                - pageDecorations.getHeaderHeight() - pageDecorations.getFooterHeight();

        final GridRenderer gridRenderer = new GridRenderer(availableWidth, availableHeight);
        gridRenderer.addRows(writeHeader(trip, doc, totals));

        // Summary == Categories table
        if (purchaseWallet.hasActivePurchase(InAppPurchase.SmartReceiptsPlus) &&
                preferenceManager.get(UserPreference.PlusSubscription.CategoricalSummationInReports)
                && !categories.isEmpty()) {
            gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_NORMAL)));
            gridRenderer.addRow(constructTitleGridRowRenderer(doc, pdfBoxContext.getString(R.string.report_summary_title)));
            gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_SMALL)));
            gridRenderer.addRows(writeCategoriesTable(categories, doc));
        }

        // Distance Table
        if (preferenceManager.get(UserPreference.Distance.PrintDistanceTableInReports) && !distances.isEmpty()) {
            gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_NORMAL)));
            gridRenderer.addRows(writeDistancesTable(distances, doc));
        }

        // Details table (receipts + distances)
        if (!receipts.isEmpty() &&
                (!purchaseWallet.hasActivePurchase(InAppPurchase.SmartReceiptsPlus) ||
                        (purchaseWallet.hasActivePurchase(InAppPurchase.SmartReceiptsPlus) &&
                                !preferenceManager.get(UserPreference.PlusSubscription.OmitDefaultTableInReports)))) {
            gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_NORMAL)));
            gridRenderer.addRow(constructTitleGridRowRenderer(doc, pdfBoxContext.getString(R.string.report_details_title)));
            gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_SMALL)));
            gridRenderer.addRows(writeReceiptsTable(receipts, doc));
        }


        if (purchaseWallet.hasActivePurchase(InAppPurchase.SmartReceiptsPlus) &&
                preferenceManager.get(UserPreference.PlusSubscription.SeparateByCategoryInReports)
                && !groupingResults.isEmpty()) {

            for (CategoryGroupingResult groupingResult : groupingResults) {
                gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_NORMAL)));


                gridRenderer.addRow(constructTitleGridRowRenderer(doc, groupingResult.getCategory().getName()));
                gridRenderer.addRow(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_SMALL)));
                gridRenderer.addRows(writeSeparateCategoryTable(groupingResult.getReceipts(), doc));
            }
        }

        gridRenderer.measure();
        gridRenderer.render(this.writer);

        // reset the page size if necessary
        if (preferenceManager.get(UserPreference.ReportOutput.PrintReceiptsTableInLandscape)) {
            pdfBoxContext.setPageSize(new PDRectangle(pdfBoxContext.getPageSize().getHeight(),
                    pdfBoxContext.getPageSize().getWidth()));
        }
    }

    private List<GridRowRenderer> writeHeader(@NonNull Trip trip, @NonNull PDDocument pdDocument, @NonNull ReceiptsTotals data) throws IOException {

        // Print the report name as the title field
        final List<GridRowRenderer> headerRows = new ArrayList<>();

        final DateFormatter dateFormatter = pdfBoxContext.getDateFormatter();


        /*User info section*/
        final String userName = preferenceManager.get(UserPreference.ReportOutput.UserName);
        final String userEmail = preferenceManager.get(UserPreference.ReportOutput.UserEmail);
        final String userId = preferenceManager.get(UserPreference.ReportOutput.UserId);
        final String department = preferenceManager.get(UserPreference.ReportOutput.Department);

        headerRows.add(constructHeaderGridRowRenderer(pdDocument, pdfBoxContext.getString(R.string.report_header_employee_name, userName), PdfFontStyle.DefaultBold));
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, pdfBoxContext.getString(R.string.report_header_email, userEmail), PdfFontStyle.Default));
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, pdfBoxContext.getString(R.string.report_header_employee_id, userId), PdfFontStyle.Default));
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, pdfBoxContext.getString(R.string.report_header_department, department), PdfFontStyle.Default));
        headerRows.add(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_SMALL)));

        /*Invoice section*/
        final DisplayableDate today = new DisplayableDate(new Date(Calendar.getInstance().getTimeInMillis()), TimeZone.getDefault());
        final String invoiceDate = pdfBoxContext.getString(R.string.report_header_invoice_date, dateFormatter.getFormattedDate(today));
        final String invoiceNumber = pdfBoxContext.getString(R.string.report_header_invoice_number, userId.replace(" ", "_"), dateFormatter.getFormattedDate(today, DateFormatter.DateFormatOption.yyyMMdd));

        headerRows.add(constructHeaderGridRowRenderer(pdDocument, invoiceDate, PdfFontStyle.Default));
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, invoiceNumber, PdfFontStyle.Default));
        // Print the various tax totals if the IncludeTaxField is true and we have taxes
        if (preferenceManager.get(UserPreference.Receipts.IncludeTaxField) && !ModelUtils.isPriceZero(data.getTaxPrice())) {

            // Print total WITHOUT taxes
            headerRows.add(constructHeaderGridRowRenderer(pdDocument,
                    pdfBoxContext.getString(R.string.report_header_grand_total_no_tax, data.getGrandTotalWithOutTaxPrice().getCurrencyFormattedPrice()),
                    PdfFontStyle.Default));

            // Print taxes
            headerRows.add(constructHeaderGridRowRenderer(pdDocument,
                    pdfBoxContext.getString(R.string.report_header_receipts_total_tax, data.getTaxPrice().getCurrencyFormattedPrice()),
                    PdfFontStyle.Default));

        }
        // Print the grand total
        headerRows.add(constructHeaderGridRowRenderer(pdDocument,
                pdfBoxContext.getString(R.string.report_header_grand_total, data.getGrandTotalPrice().getCurrencyFormattedPrice()),
                PdfFontStyle.DefaultBold));
        // Print report currency
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, pdfBoxContext.getString(R.string.RECEIPTMENU_FIELD_CURRENCY) + ": " + trip.getTripCurrency().getCurrencyCode(), PdfFontStyle.Default));
        headerRows.add(new GridRowRenderer(new EmptyRenderer(0, EMPTY_ROW_HEIGHT_SMALL)));


        /*Report Title*/
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, pdfBoxContext.getString(R.string.report_header_report_title, trip.getName()), PdfFontStyle.DefaultBold));

        // Print the From: StartDate To: EndDate
        final String fromToPeriod = pdfBoxContext.getString(R.string.report_header_duration,
                dateFormatter.getFormattedDate(trip.getStartDisplayableDate()),
                dateFormatter.getFormattedDate(trip.getEndDisplayableDate()));
        headerRows.add(constructHeaderGridRowRenderer(pdDocument, fromToPeriod, PdfFontStyle.Default));

        // Print the cost center (if present)
        if (preferenceManager.get(UserPreference.General.IncludeCostCenter) && !TextUtils.isEmpty(trip.getCostCenter())) {
            headerRows.add(constructHeaderGridRowRenderer(pdDocument,
                    pdfBoxContext.getString(R.string.report_header_cost_center, trip.getCostCenter()),
                    PdfFontStyle.Default));
        }

        // Print the report comment (if present)
        if (!TextUtils.isEmpty(trip.getComment())) {
            headerRows.add(constructHeaderGridRowRenderer(pdDocument,
                    pdfBoxContext.getString(R.string.report_header_comment, trip.getComment()),
                    PdfFontStyle.Default));
        }


        // Print the grand total (reimbursable)
        if (!preferenceManager.get(UserPreference.Receipts.OnlyIncludeReimbursable) && !data.getGrandTotalPrice().equals(data.getReimbursableGrandTotalPrice())) {
            headerRows.add(constructHeaderGridRowRenderer(pdDocument,
                    pdfBoxContext.getString(R.string.report_header_receipts_total_reimbursable, data.getReimbursableGrandTotalPrice().getCurrencyFormattedPrice()),
                    PdfFontStyle.DefaultBold));
        }


        for (final GridRowRenderer headerRow : headerRows) {
            headerRow.getRenderingFormatting().addFormatting(new Alignment(Alignment.Type.Start));
        }
        return headerRows;
    }

    private GridRowRenderer constructHeaderGridRowRenderer(@NonNull PDDocument pdDocument, @NonNull String headerText, @NonNull PdfFontStyle fontStyle) {
        return new GridRowRenderer(new TextRenderer(
                pdfBoxContext.getAndroidContext(),
                pdDocument,
                headerText,
                pdfBoxContext.getColorManager().getColor(PdfColorStyle.Default),
                pdfBoxContext.getFontManager().getFont(fontStyle)));
    }

    private GridRowRenderer constructTitleGridRowRenderer(@NonNull PDDocument doc, @NonNull String title) {
        GridRowRenderer groupTitleRenderer = new GridRowRenderer(new TextRenderer(
                pdfBoxContext.getAndroidContext(),
                doc,
                title,
                pdfBoxContext.getColorManager().getColor(PdfColorStyle.Outline),
                pdfBoxContext.getFontManager().getFont(PdfFontStyle.TableHeader)));

        groupTitleRenderer.getRenderingFormatting().addFormatting(new Alignment(Alignment.Type.Start));

        return groupTitleRenderer;
    }

    private List<GridRowRenderer> writeReceiptsTable(@NonNull List<Receipt> receipts, @NonNull PDDocument pdDocument) throws IOException {

        final List<Receipt> receiptsTableList = new ArrayList<>(receipts);
        if (preferenceManager.get(UserPreference.Distance.PrintDistanceAsDailyReceiptInReports)) {
            receiptsTableList.addAll(new DistanceToReceiptsConverter(pdfBoxContext.getAndroidContext(), pdfBoxContext.getDateFormatter(), preferenceManager).convert(distances));
            Collections.sort(receiptsTableList, new ReceiptDateComparator());
        }

        final PdfTableGenerator<Receipt> pdfTableGenerator = new PdfTableGenerator<>(pdfBoxContext,
                reportResourcesManager, receiptColumns, pdDocument, new LegacyReceiptFilter(preferenceManager),
                true, true);

        return pdfTableGenerator.generate(receiptsTableList);
    }

    private List<GridRowRenderer> writeDistancesTable(@NonNull List<Distance> distances, @NonNull PDDocument pdDocument) throws IOException {
        final PdfTableGenerator<Distance> pdfTableGenerator = new PdfTableGenerator<>(pdfBoxContext,
                reportResourcesManager, distanceColumns, pdDocument, null, true, true);
        return pdfTableGenerator.generate(distances);
    }

    private List<GridRowRenderer> writeCategoriesTable(@NonNull List<SumCategoryGroupingResult> categories, @NonNull PDDocument pdDocument) throws IOException {

        final PdfTableGenerator<SumCategoryGroupingResult> pdfTableGenerator = new PdfTableGenerator<>(pdfBoxContext,
                reportResourcesManager, categoryColumns, pdDocument, null, true, true);

        return pdfTableGenerator.generate(categories);
    }

    private List<GridRowRenderer> writeSeparateCategoryTable(@NonNull List<Receipt> receipts, @NonNull PDDocument pdDocument) throws IOException {

        final PdfTableGenerator<Receipt> pdfTableGenerator = new PdfTableGenerator<>(pdfBoxContext,
                reportResourcesManager, receiptColumns, pdDocument, null, true, true);

        return pdfTableGenerator.generate(receipts);
    }

}
