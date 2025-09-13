package dora.widget

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatRadioButton
import dora.widget.iconlabelview.R

class DoraIconLabelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatRadioButton(context, attrs, defStyleAttr) {

    private var textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var textRect = Rect()

    private var iconBitmap: Bitmap
    private var iconRect = Rect()
    private var iconDrawRect = Rect()

    private var iconLabelGap: Int
    private var iconSize: Int

    private var text: String = ""
    private var labelTextSize: Float = 12f
    private var labelTextColor: Int = Color.BLACK

    private var iconBackgroundShape: Int = SHAPE_NONE
    private var iconBackgroundColor: Int = Color.LTGRAY
    private var iconBackgroundPadding: Int = 0
    private var iconBackgroundBorder: Boolean = false
    private var iconCornerRadius: Float = 0f
    private var iconBackgroundBorderColor: Int = Color.DKGRAY
    private var iconBackgroundBorderWidth: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
    )

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    companion object {
        const val SHAPE_NONE = 0
        const val SHAPE_ROUNDED_RECT = 1
        const val SHAPE_CIRCLE = 2
    }

    fun setIconBitmap(bitmap: Bitmap) {
        this.iconBitmap = bitmap
        invalidateView()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textPaint.getTextBounds(text, 0, text.length, textRect)
        val bgSize = if (iconBackgroundShape == SHAPE_NONE) iconSize else iconSize + 2 * iconBackgroundPadding

        val desiredWidth = bgSize.coerceAtLeast(textRect.width()) + paddingLeft + paddingRight
        val desiredHeight = bgSize + iconLabelGap + textRect.height() + paddingTop + paddingBottom

        val viewWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val viewHeight = resolveSize(desiredHeight, heightMeasureSpec)

        val iconLeft = paddingLeft + (viewWidth - paddingLeft - paddingRight - bgSize) / 2
        val iconTop = paddingTop
        iconRect.set(iconLeft, iconTop, iconLeft + bgSize, iconTop + bgSize)

        iconDrawRect.set(
            iconRect.left + iconBackgroundPadding,
            iconRect.top + iconBackgroundPadding,
            iconRect.right - iconBackgroundPadding,
            iconRect.bottom - iconBackgroundPadding
        )

        val textLeft = paddingLeft + (viewWidth - paddingLeft - paddingRight - textRect.width()) / 2
        val textTop = iconRect.bottom + iconLabelGap
        textRect.offsetTo(textLeft, textTop)

        setMeasuredDimension(viewWidth, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        drawIconWithBackground(canvas)
        drawText(canvas)
    }

    private fun drawIconWithBackground(canvas: Canvas) {
        if (iconBackgroundShape != SHAPE_NONE) {
            bgPaint.color = iconBackgroundColor
            borderPaint.color = iconBackgroundBorderColor
            borderPaint.strokeWidth = iconBackgroundBorderWidth

            when (iconBackgroundShape) {
                SHAPE_ROUNDED_RECT -> {
                    canvas.drawRoundRect(
                        iconRect.left.toFloat(),
                        iconRect.top.toFloat(),
                        iconRect.right.toFloat(),
                        iconRect.bottom.toFloat(),
                        iconCornerRadius,
                        iconCornerRadius,
                        bgPaint
                    )
                    if (iconBackgroundBorder) {
                        canvas.drawRoundRect(
                            iconRect.left.toFloat(),
                            iconRect.top.toFloat(),
                            iconRect.right.toFloat(),
                            iconRect.bottom.toFloat(),
                            iconCornerRadius,
                            iconCornerRadius,
                            borderPaint
                        )
                    }
                }
                SHAPE_CIRCLE -> {
                    val cx = iconRect.exactCenterX()
                    val cy = iconRect.exactCenterY()
                    val radius = iconRect.width().coerceAtMost(iconRect.height()) / 2f
                    canvas.drawCircle(cx, cy, radius, bgPaint)
                    if (iconBackgroundBorder) {
                        canvas.drawCircle(cx, cy, radius, borderPaint)
                    }
                }
            }
        }
        canvas.drawBitmap(iconBitmap, null, iconDrawRect, null)
    }

    private fun drawText(canvas: Canvas) {
        textPaint.color = labelTextColor
        val baseline = textRect.top - textPaint.fontMetrics.top
        canvas.drawText(text, textRect.left.toFloat(), baseline, textPaint)
    }

    private fun invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DoraIconLabelView, defStyleAttr, 0)

        val drawable = a.getDrawable(R.styleable.DoraIconLabelView_dview_ilv_icon)
            ?: throw IllegalArgumentException("icon attribute is required.")
        iconSize = a.getDimensionPixelSize(
            R.styleable.DoraIconLabelView_dview_ilv_iconSize,
            drawable.intrinsicWidth.coerceAtLeast(drawable.intrinsicHeight)
        )
        iconBitmap = Bitmap.createBitmap(
            iconSize, iconSize,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(iconBitmap)
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.draw(canvas)

        text = a.getString(R.styleable.DoraIconLabelView_dview_ilv_text).orEmpty()
        iconLabelGap = a.getDimensionPixelSize(
            R.styleable.DoraIconLabelView_dview_ilv_iconLabelGap,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
        )
        labelTextSize = a.getDimension(
            R.styleable.DoraIconLabelView_dview_ilv_textSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        )
        labelTextColor = a.getColor(
            R.styleable.DoraIconLabelView_dview_ilv_textColor,
            textColors.defaultColor
        )

        iconBackgroundShape = a.getInt(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundShape, SHAPE_NONE)
        iconBackgroundColor = a.getColor(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundColor, Color.LTGRAY)
        iconBackgroundPadding = a.getDimensionPixelSize(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundPadding, 0)
        iconBackgroundBorder = a.getBoolean(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundBorder, false)
        iconCornerRadius = a.getDimension(
            R.styleable.DoraIconLabelView_dview_ilv_iconCornerRadius,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
        )

        iconBackgroundBorderColor = a.getColor(
            R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundBorderColor,
            Color.DKGRAY
        )
        iconBackgroundBorderWidth = a.getDimension(
            R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundBorderWidth,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
        )

        a.recycle()

        textPaint.textSize = labelTextSize
        textPaint.color = labelTextColor
    }
}
