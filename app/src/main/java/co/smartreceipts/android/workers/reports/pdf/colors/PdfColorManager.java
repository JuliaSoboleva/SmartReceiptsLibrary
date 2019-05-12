package co.smartreceipts.android.workers.reports.pdf.colors;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.tom_roush.pdfbox.util.awt.AWTColor;

import java.util.HashMap;
import java.util.Map;

public class PdfColorManager {

    private final Map<PdfColorStyle, AWTColor> colors = new HashMap<>();

    public PdfColorManager() {
        colors.put(PdfColorStyle.Outline, new AWTColor(0, 191, 165)); // Yooz green color
        colors.put(PdfColorStyle.TableHeader, new AWTColor(218, 244, 242));
        colors.put(PdfColorStyle.TableCell, new AWTColor(245, 245, 245));
        colors.put(PdfColorStyle.Default, new AWTColor(60, 72, 87)); // Yooz primary text color
    }

    @NonNull
    public AWTColor getColor(@NonNull PdfColorStyle style) {
        return Preconditions.checkNotNull(colors.get(style));
    }
}
