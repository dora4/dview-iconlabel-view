package dora.widget

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatRadioButton
import dora.widget.iconlabelview.R
import kotlin.math.ceil

class DoraIconLabelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatRadioButton(context, attrs, defStyleAttr) {

    private var textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var textRect = Rect()
    private var iconRect = Rect()
    private var iconDrawRect = Rect()

    private var iconLabelGap: Int
    private var iconScaleX = 1f
    private var iconScaleY = 1f
    private lateinit var cacheBitmap: Bitmap
    private var iconBitmap: Bitmap

    // 背景配置
    private var iconBackgroundColor: Int = Color.LTGRAY
    private var iconBackgroundShape: Int = SHAPE_ROUNDED_RECT
    private var iconCornerRadius: Float = 12f
    private var iconPadding: Int = 0
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    companion object {
        const val SHAPE_ROUNDED_RECT = 0
        const val SHAPE_CIRCLE = 1
    }

    fun setIconBitmap(bitmap: Bitmap) {
        this.iconBitmap = bitmap
        invalidateView()
    }

    /**
     * 选中比例（0~1），用于过渡
     */
    var ratio: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateView()
            }
        }

    private var text: String = ""
        set(value) {
            if (field != value) {
                field = value
                invalidateView()
            }
        }

    private var labelTextSize: Float = 12f
        set(value) {
            if (field != value) {
                field = value
                textPaint.textSize = field
                invalidateView()
            }
        }

    private var labelTextColor: Int = Color.BLACK
        set(value) {
            if (field != value) {
                field = value
                textPaint.color = field
                invalidateView()
            }
        }

    private var hoverColor: Int = Color.BLACK
        set(value) {
            if (field != value) {
                field = value
                invalidateView()
            }
        }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        ratio = if (checked) 1f else 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val iconWidth = iconBitmap.width
        val iconHeight = iconBitmap.height
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        val viewWidth = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            iconWidth.coerceAtLeast(textBounds.width()) + paddingLeft + paddingRight
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }
        val viewHeight = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            iconHeight + iconLabelGap + textBounds.height() + paddingTop + paddingBottom
        } else {
            MeasureSpec.getSize(heightMeasureSpec)
        }

        val iconLeft = paddingLeft + (viewWidth - paddingLeft - paddingRight - iconWidth) / 2
        val iconRight = iconLeft + iconWidth
        val textLeft = paddingLeft + (viewWidth - paddingLeft - paddingRight - textBounds.width()) / 2
        val textRight = textLeft + textBounds.width()
        val totalHeight = iconHeight + iconLabelGap + textBounds.height()
        val iconTop = paddingTop + (viewHeight - paddingTop - paddingBottom - totalHeight) / 2
        val textTop = iconTop + iconHeight + iconLabelGap

        iconRect.set(iconLeft, iconTop, iconRight, iconTop + iconHeight)
        textRect.set(textLeft, textTop, textRight, textTop + textBounds.height())

        // icon 实际绘制区域（考虑内边距）
        iconDrawRect.set(
            iconRect.left + iconPadding,
            iconRect.top + iconPadding,
            iconRect.right - iconPadding,
            iconRect.bottom - iconPadding
        )

        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val alpha = ceil((255 * ratio).toDouble()).toInt()
        resetIcon(canvas)
        drawIcon(alpha)
        drawCacheText(canvas, alpha)
        drawHoverText(canvas, alpha)
    }

    private fun resetIcon(canvas: Canvas) {
        // 绘制背景
        bgPaint.color = iconBackgroundColor
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
            }
            SHAPE_CIRCLE -> {
                val cx = iconRect.exactCenterX()
                val cy = iconRect.exactCenterY()
                val radius = (iconRect.width().coerceAtMost(iconRect.height())) / 2f
                canvas.drawCircle(cx, cy, radius, bgPaint)
            }
        }
        // 绘制 icon
        canvas.drawBitmap(iconBitmap, null, iconDrawRect, null)
    }

    private fun drawIcon(alpha: Int) {
        cacheBitmap = Bitmap.createBitmap(
            measuredWidth,
            measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(cacheBitmap)
        iconPaint.reset()
        iconPaint.color = hoverColor
        iconPaint.isDither = true
        iconPaint.alpha = alpha
        canvas.drawRect(iconRect, iconPaint)
        iconPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        iconPaint.alpha = 255
        canvas.drawBitmap(iconBitmap, null, iconDrawRect, iconPaint)
    }

    private fun drawCacheText(canvas: Canvas, alpha: Int) {
        textPaint.color = labelTextColor
        textPaint.alpha = 255 - alpha
        val topY = textRect.top
        val baselineY: Float = topY - textPaint.fontMetrics.top
        for (index in 0..text.length) {
            val subText = text.substring(0, text.length - index)
            val textWidth = textPaint.measureText(subText)
            if (subText.isNotEmpty()) {
                val lastTextWidth = textPaint.measureText(subText.last().toString())
                if (textWidth < measuredWidth - paddingLeft - paddingRight + lastTextWidth) {
                    textRect.left =
                        paddingLeft + ((measuredWidth - paddingLeft - paddingRight - textWidth) / 2).toInt()
                    canvas.drawText(
                        subText, textRect.left.toFloat(), baselineY - textRect.height() / 2, textPaint
                    )
                    return
                }
            }
        }
    }

    private fun drawHoverText(canvas: Canvas, alpha: Int) {
        textPaint.color = hoverColor
        textPaint.alpha = alpha
        val topY = textRect.top
        val baselineY: Float = topY - textPaint.fontMetrics.top
        for (index in 0..text.length) {
            val subText = text.substring(0, text.length - index)
            val textWidth = textPaint.measureText(subText)
            if (subText.isNotEmpty()) {
                val lastTextWidth = textPaint.measureText(subText.last().toString())
                if (textWidth < measuredWidth - paddingLeft - paddingRight + lastTextWidth) {
                    textRect.left =
                        paddingLeft + ((measuredWidth - paddingLeft - paddingRight - textWidth) / 2).toInt()
                    canvas.drawText(
                        subText, textRect.left.toFloat(), baselineY - textRect.height() / 2, textPaint
                    )
                    return
                }
            }
        }
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
        iconScaleX = a.getFraction(R.styleable.DoraIconLabelView_dview_ilv_iconScaleX, 1, 1, 1.0f)
        iconScaleY = a.getFraction(R.styleable.DoraIconLabelView_dview_ilv_iconScaleY, 1, 1, 1.0f)
        val drawable = a.getDrawable(R.styleable.DoraIconLabelView_dview_ilv_icon)
        if (iconScaleX == 0f || iconScaleY == 0f || drawable == null) {
            throw IllegalArgumentException("icon attribute error.")
        }
        val bmpWidth = drawable.intrinsicWidth * iconScaleX
        val bmpHeight = drawable.intrinsicHeight * iconScaleY
        iconBitmap = Bitmap.createBitmap(
            bmpWidth.toInt(), bmpHeight.toInt(),
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(iconBitmap)
        drawable.setBounds(0, 0, bmpWidth.toInt(), bmpHeight.toInt())
        drawable.draw(canvas)

        text = a.getString(R.styleable.DoraIconLabelView_dview_ilv_text).orEmpty()
        hoverColor = a.getColor(R.styleable.DoraIconLabelView_dview_ilv_hoverColor, Color.BLACK)
        iconLabelGap = a.getDimensionPixelSize(
            R.styleable.DoraIconLabelView_dview_ilv_iconLabelGap,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
        )
        labelTextSize = a.getDimension(
            R.styleable.DoraIconLabelView_dview_ilv_textSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        )
        labelTextColor =
            a.getColor(R.styleable.DoraIconLabelView_dview_ilv_textColor, textColors.defaultColor)
        ratio = a.getFraction(R.styleable.DoraIconLabelView_dview_ilv_ratio, 1, 1, 0f)

        // 背景属性
        iconBackgroundColor = a.getColor(
            R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundColor,
            Color.LTGRAY
        )
        iconBackgroundShape = a.getInt(
            R.styleable.DoraIconLabelView_dview_ilv_iconBackgroundShape,
            SHAPE_ROUNDED_RECT
        )
        iconCornerRadius = a.getDimension(
            R.styleable.DoraIconLabelView_dview_ilv_iconCornerRadius,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
        )
        iconPadding = a.getDimensionPixelSize(
            R.styleable.DoraIconLabelView_dview_ilv_iconPadding,
            0
        )

        a.recycle()
        textPaint.textSize = labelTextSize
        textPaint.color = labelTextColor
    }
}
