package dora.widget

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import dora.widget.iconlabelview.R

class DoraIconLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val SHAPE_NONE = 0
        const val SHAPE_ROUNDED_RECT = 1
        const val SHAPE_CIRCLE = 2
    }

    private var text: String? = null
    private var textSize: Float = sp2px(14f)
    @ColorInt
    private var textColor: Int = Color.BLACK

    private var icon: Int = 0
    private var iconBitmap: Bitmap? = null
    private var iconSize: Int = -1
    private var iconLabelGap: Int = dp2px(4f)

    private var iconBackgroundShape: Int = SHAPE_NONE
    @ColorInt
    private var iconBackgroundColor: Int = Color.LTGRAY
    private var iconBackgroundPadding: Int = 0
    private var iconBackgroundBorder: Boolean = false
    private var iconCornerRadius: Float = dp2px(4f).toFloat()

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val iconRect = Rect()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp2px(1f).toFloat()
        color = Color.BLACK
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DoraIconLabelView)

        icon = a.getResourceId(R.styleable.DoraIconLabelView_dview_ilv_icon, 0)
        text = a.getString(R.styleable.DoraIconLabelView_dview_ilv_text)
        textSize = a.getDimension(R.styleable.DoraIconLabelView_dview_ilv_textSize, textSize)
        textColor = a.getColor(R.styleable.DoraIconLabelView_dview_ilv_textColor, textColor)
        iconSize = a.getDimensionPixelSize(R.styleable.DoraIconLabelView_dview_ilv_iconSize, -1)
        iconLabelGap = a.getDimensionPixelSize(R.styleable.DoraIconLabelView_dview_ilv_iconLabelGap, iconLabelGap)

        iconBackgroundShape = a.getInt(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundShape, SHAPE_NONE)
        iconBackgroundColor = a.getColor(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundColor, iconBackgroundColor)
        iconBackgroundPadding = a.getDimensionPixelSize(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundPadding, 0)
        iconBackgroundBorder = a.getBoolean(R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundBorder, false)
        iconCornerRadius = a.getDimension(R.styleable.DoraIconLabelView_dview_ilv_iconCornerRadius, iconCornerRadius)

        a.recycle()

        if (icon != 0) {
            val drawable = ContextCompat.getDrawable(context, icon)
            drawable?.let {
                val bmpWidth = if (iconSize > 0) iconSize else it.intrinsicWidth
                val bmpHeight = if (iconSize > 0) iconSize else it.intrinsicHeight
                iconBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(iconBitmap!!)
                it.setBounds(0, 0, bmpWidth, bmpHeight)
                it.draw(canvas)
            }
        }

        textPaint.textSize = textSize
        textPaint.color = textColor
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = iconBackgroundColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textWidth = text?.let { textPaint.measureText(it).toInt() } ?: 0
        val textHeight = text?.let { (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent).toInt() } ?: 0

        val iconW = iconBitmap?.width ?: 0
        val iconH = iconBitmap?.height ?: 0

        val contentWidth = maxOf(iconW, textWidth)
        val contentHeight = iconH + if (text != null) (iconLabelGap + textHeight) else 0

        val measuredW = resolveSize(contentWidth + paddingLeft + paddingRight, widthMeasureSpec)
        val measuredH = resolveSize(contentHeight + paddingTop + paddingBottom, heightMeasureSpec)

        setMeasuredDimension(measuredW, measuredH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val iconBmp = iconBitmap
        val textStr = text

        val centerX = width / 2f
        var top = paddingTop

        // --- 绘制 icon ---
        if (iconBmp != null) {
            val left = (centerX - iconBmp.width / 2).toInt()
            val right = left + iconBmp.width
            val bottom = top + iconBmp.height
            iconRect.set(left, top, right, bottom)

            // 背景
            if (iconBackgroundShape != SHAPE_NONE) {
                val bgLeft = iconRect.left - iconBackgroundPadding
                val bgTop = iconRect.top - iconBackgroundPadding
                val bgRight = iconRect.right + iconBackgroundPadding
                val bgBottom = iconRect.bottom + iconBackgroundPadding
                when (iconBackgroundShape) {
                    SHAPE_ROUNDED_RECT -> {
                        canvas.drawRoundRect(
                            bgLeft.toFloat(),
                            bgTop.toFloat(),
                            bgRight.toFloat(),
                            bgBottom.toFloat(),
                            iconCornerRadius,
                            iconCornerRadius,
                            bgPaint
                        )
                        if (iconBackgroundBorder) {
                            canvas.drawRoundRect(
                                bgLeft.toFloat(),
                                bgTop.toFloat(),
                                bgRight.toFloat(),
                                bgBottom.toFloat(),
                                iconCornerRadius,
                                iconCornerRadius,
                                borderPaint
                            )
                        }
                    }
                    SHAPE_CIRCLE -> {
                        val cx = (bgLeft + bgRight) / 2f
                        val cy = (bgTop + bgBottom) / 2f
                        val radius = (bgRight - bgLeft).coerceAtMost(bgBottom - bgTop) / 2f
                        canvas.drawCircle(cx, cy, radius, bgPaint)
                        if (iconBackgroundBorder) {
                            canvas.drawCircle(cx, cy, radius, borderPaint)
                        }
                    }
                }
            }

            // 绘制 icon
            canvas.drawBitmap(iconBmp, iconRect.left.toFloat(), iconRect.top.toFloat(), null)

            top = iconRect.bottom
        }

        // --- 绘制文字 ---
        if (textStr != null) {
            top += iconLabelGap
            val x = centerX - textPaint.measureText(textStr) / 2
            val y = top - textPaint.ascent() // 基线对齐
            canvas.drawText(textStr, x, y, textPaint)
        }
    }

    private fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        )
    }
}
