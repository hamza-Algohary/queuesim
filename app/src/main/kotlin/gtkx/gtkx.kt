package gtkx

import ch.bailu.gtk.adw.Clamp
import ch.bailu.gtk.adw.ToastOverlay
import ch.bailu.gtk.cairo.Context
import ch.bailu.gtk.cairo.LineCap
import ch.bailu.gtk.gdk.Display
import ch.bailu.gtk.gdkpixbuf.Pixbuf
import ch.bailu.gtk.gio.ApplicationFlags
import ch.bailu.gtk.gtk.*
import ch.bailu.gtk.type.Str
import ch.bailu.gtk.type.Strs
import ch.bailu.gtk.glib.Glib;
import ch.bailu.gtk.glib.Glib.timeoutAdd
import ch.bailu.gtk.glib.Glib.variantIsObjectPath
import kotlin.math.roundToInt


enum class Align {
     FILL , START , END ,CENTER ,BASELINE
}

enum class Overflow {
    VISIBLE , HIDDEN
}

enum class Orientation {
    HORIZONTAL , VERTICAL
}

fun <T : Widget> T.halign(halign : Align) =
    this.apply {
        this.halign = halign.ordinal
    }

fun <T : Widget> T.valign(valign : Align) =
    this.apply {
        this.valign = valign.ordinal
    }

fun <T : Widget> T.hexpand(expand : Boolean = true) =
    this.apply {
        this.hexpand = expand
    }

fun <T : Widget> T.vexpand(expand : Boolean = true) =
    this.apply {
        this.vexpand = expand
    }

fun <T : Widget> T.margins(top:Int , start:Int = top , bottom:Int = top , end:Int = start) =
    this.apply {
        this.marginStart = start
        this.marginEnd = end
        this.marginTop = top
        this.marginBottom = bottom
    }

fun <T : Widget> T.cssClasses(vararg classes : String) =
    this.apply {
        classes.forEach {
            addCssClass(it)
        }
    }
fun <T : Widget> T.name(name : String) =
    this.apply {
        this.name = Str(name)
    }

fun <T : Widget> T.opacity(opacity : Double) =
    this.apply {
        this.opacity = opacity
    }

fun <T : Widget> T.overflow(overflow : Overflow) =
    this.apply {
        this.overflow = overflow.ordinal
    }

fun <T : Widget> T.tooltips(tips : String , useMarkUp : Boolean = true) =
    apply {
        if(useMarkUp)
            tooltipMarkup = Str(tips)
        else
            tooltipText = Str(tips)
    }
fun <T : Widget> T.size(width : Int , height : Int = width)=
    this.apply {
        this.setSizeRequest(width , height)
    }

fun <T : Widget> T.css(styleSheet : String)=
    this.apply {
        this.style = styleSheet
    }

fun <T : Widget> T.id(id : String)=
    this.apply {
        this.ID = id
    }

fun hbox(spacing: Int = 0) = Box(Orientation.HORIZONTAL.ordinal , spacing)
fun vbox(spacing: Int = 0) = Box(Orientation.VERTICAL.ordinal , spacing)

fun Label.markup(use: Boolean) =
    this.apply {
        this.useMarkup = use
    }

fun Frame.label(text : String) =
    this.apply {
        this.label = Str(text)
    }

fun Expander.label(text : String) =
    this.apply {
        this.label = Str(text)
    }


val globalStylesMap : HashMap<Widget , String> = HashMap()

val widgetsIDs : HashMap<String , Widget> = HashMap()

fun <K,V> HashMap<K,V>.getFirstKeyOfValue(value : V , default : K) : K = this.filterValues { it == value }.keys.elementAtOrElse(0) { default }

var globalStyleSheet = ""
fun getCustomStyle() =
    globalStyleSheet +
    globalStylesMap.map {
        //if(it.key.name.toString().isEmpty())
        it.key.name(uniqueWidgetName(it.key.name.toString()))
        "#"+it.key.name.toString() + "{" + it.value + "}"
    }.joinToString().also{ println("Style = " + it) }

//fun Window.applyStyle(styleSheet : String = getCustomStyle()) {
//    println(styleSheet)
//    StyleContext.addProviderForDisplay(
//        this.display,
//        CssProvider().apply{
//            loadFromData(styleSheet , styleSheet.length.toLong())
//        }.asStyleProvider(),
//        10000
//    )
//}

infix fun <T : Widget> String.at(widget : T) = widget.id(this)

fun applyStyle(styleSheet : String = getCustomStyle()) {
    println(styleSheet)
    StyleContext.addProviderForDisplay(
        Display.getDefault(),
        CssProvider().apply{
            loadFromData(styleSheet , styleSheet.length.toLong())
        }.asStyleProvider(),
        10000
    )
}

var Widget.style : String
    get() {
        return globalStylesMap.getOrDefault(this, "")
    }
    set(value) {
        globalStylesMap.put(this, value)
    }

val <T : Widget> T.t : T
    get () = this

var Widget.ID : String
    get() {
        return widgetsIDs.getFirstKeyOfValue(this , "")
    }
    set(value) {
        widgetsIDs.put(value , this)
    }

fun getWidgetByID(id:String) : Widget? {
    return widgetsIDs.getOrElse(id) {
        println("ID $id Not Found")
        null
    }
}

fun <T> getWidgetByID(id : String) : T {
    return getWidgetByID(id) as T
}

//fun <T> getWidgetByID(id : String , defaultValue: T) : T {
//    val widget = getWidgetByID(id)
//    return if (widget::class == defaultValue!!::class) {
//        getWidgetByID(id) as T
//    } else {
//        defaultValue
//    }
//}

private var nameCounter = -1
fun uniqueWidgetName(name : String = "widget") : String {
    nameCounter++
    return "$name$nameCounter"
}


fun DrawingArea.onDraw(handler : Context.(width:Int, height:Int)->Unit) = this.apply {
    this.setDrawFunc(
        { _, _, context, width, height, _ ->context.handler(width, height)} ,
        null,
        {_, _ -> this.unregisterCallbacks();}
    )
}

fun application(id : String, flags : Int = ApplicationFlags.DEFAULT_FLAGS, args:Array<String> = arrayOf(), func:Application.()->Unit ) =
    ch.bailu.gtk.adw.Application(id, flags).apply(func).run(args.size, Strs(args))

fun Application.window(title:String="", id:String = "" , func : ApplicationWindow.()->Unit = {}) =
    ApplicationWindow(this).apply{
        this.title = Str(title)
        this.ID = id
        this.func()
        this.show()
        applyStyle()
    }



fun <T : Button> T.action(handler: T.() -> Unit) = this.apply {
    onClicked { handler() }
}

//fun ToggleButton.onToggle(handler: ToggleButton.() -> Unit) = this.apply {
//    onClicked { handler() }
//}

fun <T:Widget> T.keyboard(func : EventControllerKey.()->Unit = {}) {
    this.addController(EventControllerKey().apply(func))
}

fun EventControllerKey.onPress(handler : (keyValue : Int,  keyCode : Int) -> Boolean) {
    this.onKeyPressed({x,y,_ -> handler(x,y)})
}

fun EventControllerKey.onRelease(handler : (keyValue : Int,  keyCode : Int) -> Boolean) {
    this.onKeyReleased({x,y,_ -> handler(x,y)})
}

var EditableLabel.text
    get() = this.asEditable().getText().toString()
    set(value) = this.asEditable().setText(Str(value))

fun EditableLabel.onChange(handler : ()->Unit) = this.apply {
    asEditable().onChanged(handler)
}

public data class Color(var red:Int , var green:Int , var blue:Int , var alpha:Double=1.0) {
    init {
        if (red > 255) red = 255
        if (green > 255) green = 255
        if (blue > 255) blue = 255
        if (alpha > 1.0) alpha = 1.0
    }
    fun isValid() = red >= 0 && green >= 0 && blue >= 0 && alpha >= 0
    fun red() = red/255.0
    fun green() = green/255.0
    fun blue() = blue/255.0
}
public data class Point(var x : Double , var y: Double)

fun Popover.parent(widget : Widget) = this.apply {
    setParent(widget)
}

fun Popover.child(widget : Widget) = this.apply {
    child = widget
}

fun Frame.child(widget : Widget) = this.apply {
    this.setChild(widget)
}

fun Expander.child(child:Widget) = this.apply{
    setChild(child)
}
fun ScrolledWindow.child(child:Widget) = this.apply{
    setChild(child)
}
fun WindowHandle.child(child:Widget) = this.apply{
    setChild(child)
}
fun Clamp.child(child:Widget) = this.apply{
    setChild(child)
}
fun ToastOverlay.child(child:Widget) = this.apply{
    setChild(child)
}
fun Window.child(child:Widget) = this.apply{
    setChild(child)
}
fun Window.titlebar(widget : Widget) = this.apply {
    setTitlebar(widget)
}

fun Box.children(vararg children : Widget) = this.apply {
    for (child in children)
        append(child)
}

fun FlowBox.children(vararg children : Widget) = this.apply {
    for (child in children)
        append(child)
}

fun HeaderBar.start(vararg children: Widget) = this.apply {
    children.forEach {
        packStart(it)
    }
}

fun HeaderBar.center(widget: Widget) = this.apply {
    titleWidget = widget
}

fun HeaderBar.end(vararg children: Widget) = this.apply {
    children.forEach {
        packEnd(it)
    }
}

fun Pixbuf.recolor(handler:(x:Int, y:Int, oldColor:Color)->Color) = this.apply {

}

fun Context.line(xStart:Double ,  yStart:Double ,  xEnd:Double ,  yEnd:Double) {
    moveTo(xStart.toDouble() , yStart.toDouble())
    lineTo(xEnd.toDouble() , yEnd.toDouble())
    stroke()
}


fun Context.settings(lineCap : Int = -1,
                     lineJoin: Int = -1,
                     red : Int = -1 ,
                     green : Int = -1 ,
                     blue : Int = -1 ,
                     alpha: Double = 1.0 ,
                     color : Color = Color(red , green , blue , alpha),
                     lineWidth : Double = -1.0,
                     fontSize : Double = -1.0,
) {
    if(lineWidth > 0)
        this.setLineWidth(lineWidth)
    if(lineCap > 0)
        this.setLineCap(lineCap)
    if(lineJoin > 0)
        this.setLineJoin(lineJoin)
    if(color.isValid())
        this.setSourceRgba(color.red() , color.green() , color.blue() , alpha)
    if(fontSize > 0)
        setFontSize(fontSize)
}


//fun DrawingArea.startAnimation() = this.apply {
//    ID = addTickCallback({_,_,_,_ ->
//        queueDraw()
//        true
//    } , null , null ).toString()
//}
//
//fun DrawingArea.stopAnimation() = this.apply {
//    try {
//        removeTickCallback(ID.toInt())
//    } catch (e : Exception) {
//        println("Animation didn't successfully stop.")
//    }
//}

fun Widget.addTickCallback(handler: () -> Unit) =
    addTickCallback({_,_,_,_ ->
        handler()
        true
    } , null , null )

val DrawingArea.tickCallBackID get() = integerProperty("tickCallBackID")
val DrawingArea.animationRunning get() =
    booleanProperty("animationRunning") { _ , running ->
        if(running) {
            tickCallBackID.set(
                addTickCallback{queueDraw()}
            )
        } else {
            removeTickCallback(tickCallBackID.get())
        }
    }

fun <T : Button> T.label(text : String) = this.apply {
    this.text.set(text)
}

fun <T : Button> T.icon(name : String) = this.apply {
    this.icon.set(name)
}

val Button.icon get() = stringProperty("icon") {_ , value ->
    iconName = Str(value)
}

val Button.text get() = stringProperty("text") {_ , value ->
    label = Str(value)
}

fun ToggleButton.active(yes : Boolean = true) = apply { active = yes }
private fun ToggleButton.updateIcons(activeIcon : String , inActiveIcon : String) = this.apply {
    icon(if(active) { activeIcon } else {inActiveIcon})
}
fun ToggleButton.icons(activeIcon : String , inActiveIcon : String) = this.apply{
    updateIcons(activeIcon , inActiveIcon)
    action{
        updateIcons(activeIcon , inActiveIcon)
    }
}

fun ToggleButton.bind(property : ()->Property<Boolean>) = this.apply {
    action{
        property().set(active)
    }
}

fun Paned.start(widget : Widget) = this.apply {
    startChild = widget
}
fun Paned.end(widget : Widget) = this.apply {
    endChild = widget
}

fun Paned.direction(dir : Int) = this.apply {
    direction = dir
}

fun Collection<String>.toLabels() = map { Label(Str(it)) }

public class UpdatableBin(var child : (()->Widget)? = null) : Box(Orientation.HORIZONTAL.ordinal , 0) {
    fun update() {
        remove(firstChild)
        if(child != null)
            append(child!!())
    }
}

fun UpdatableBin.child(widget : ()->Widget) = this.apply {
    child = widget
}

fun timer(milliseconds : Int , handler : ()->Boolean) =
    timeoutAdd(milliseconds, {_, _ -> handler()} , null)

fun Collection<Collection<Widget>>.toGrid() = Grid().also {
    this.forEachIndexed { i , row ->
        row.forEachIndexed { j , cell ->
            it.attach(cell , j , i , 1 , 1)
        }
    }
}

fun Grid.setValue(x : Int , y : Int , widget : Widget) {
    attach(widget , x , y , width, height)
}

fun Grid.child(x : Int , y : Int , widget : Widget , width: Int = 1 , height: Int = 1) = this.apply{
    attach(widget,x,y,width, height)
}

fun Grid.rowHomogenous(homo : Boolean) = this.apply { rowHomogeneous = homo }
fun Grid.columnHomogenous(homo : Boolean) = this.apply { columnHomogeneous = homo }
fun Grid.columnSpacing(spacing: Int) = this.apply { columnSpacing = spacing }
fun Grid.rowSpacing(spacing: Int) = this.apply { rowSpacing = spacing }

fun Window.size(width : Int , height : Int) = this.apply { setDefaultSize(width, height) }

fun ScrolledWindow.policy( hPolicy: Int , vPolicy: Int) = this.apply {
    setPolicy(hPolicy , vPolicy)
}

fun Context.plot(points : List<Pair<Double , Double>> , width : Int , height : Int, xScale : Double = 1.0/*/Math.max(points.size.toDouble() / width , 1.0) */, yScale : Double = 1.0 , show : Boolean = true) : Pair<Double , Double> {
    moveTo(0.0,height/2.0)
    val xScale = xScale.coerceAtMost(width/points.size.toDouble())
    if(show) {
        points.forEachSample(numberOfSamples = width) {
            //println("${it.first},${it.second} as ${it.first*xScale} ,${it.second*-yScale + height/2}")
            lineTo(it.first*xScale ,it.second*-yScale + height/2)
        }
        stroke()
    }
    return Pair(xScale , yScale)
}

fun Context.plotXAxis(width : Int , height: Int , step : Int , scale : Double , xDisplacement: Double , yDisplacement : Double , textDisplacement : Double = -3.0 , valueRepresenter : (Double)->String = {it -> it.toInt().toString()}) {
    line(0.toDouble() , yDisplacement , width.toDouble() , yDisplacement)
    for(i in 0..width step step) {
        if (i==0) continue
        moveTo(i.toDouble() /*+ xDisplacement*/, yDisplacement+textDisplacement)
        showText(valueRepresenter(i/scale + xDisplacement/scale))
    }
}

fun Context.plotYAxis(width : Int , height: Int , step : Int , scale : Double , xDisplacement : Double , yDisplacement : Double , textDisplacement: Double = 3.0 , valueRepresenter : (Double)->String = {it -> it.toInt().toString()} , showAxis : Boolean = true) {
    if(showAxis)
        line(xDisplacement , 0.toDouble() , xDisplacement , height.toDouble())
    for(i in 0..height step step) {
        if (i==0) continue
        moveTo(xDisplacement+textDisplacement , i.toDouble())
        showText(valueRepresenter(i/scale + yDisplacement/-scale))
    }
}

//    for (i in 1..<points.size step Math.max(points.size / width , 1)) {
//        lineTo(i*xScale , points[i].second*-yScale + height/2)
//        //println("Plotting ${points[i]} as ${points[i].scaled(xScale, -yScale).translated(0.0 , -height.toDouble())}")
//        //line((i-1)*xScale , points[i-1].second*-yScale + height/2.0, i*xScale , points[i].second*-yScale + height/2)
//        //point(points[i].scaled(xScale, -yScale).translated(0.0 , -height.toDouble()))
//    }
//}

fun Context.point(p : Pair<Double , Double>) = point(p.first , p.second)
fun Context.point(x : Double , y : Double) = circle(x,y,1.0)

fun Context.circle(x : Double , y : Double , radius : Double , operation : Context.()->Any = Context::fill) {
    arc(x,y,radius,0.0,2*Math.PI)
    operation()
}

fun Pair<Double , Double>.scaled(xScale : Double , yScale : Double = xScale) = Pair(first*xScale , second*yScale)
fun Pair<Double , Double>.translated(x : Double , y : Double) = Pair(first+x , second+y)

fun <T> List<T>.forEachStepped(stepSize : Int , handler : (it : T)->Unit ) {
    for(i in indices step stepSize.coerceAtLeast(1)) {
        handler(get(i))
    }
}
fun <T> List<T>.forEachSample(numberOfSamples : Int , handler : (it : T)->Unit ) = forEachStepped(size/numberOfSamples.coerceAtLeast(1) , handler)

fun spinButton(value : Double , lower : Double , upper : Double , step: Double , digits : Int) = SpinButton(Adjustment(value , lower , upper , step , step , 0.0) , step , digits)
fun SpinButton.onChange(handler: SpinButton.() -> Unit) = this.apply {
    handler()
    onValueChanged {
        handler()
    }
}

fun ToggleButton.popover(offsetX : Int = 0 , offsetY : Int = 0 , popoverHandler : Popover.()->Unit) = this.apply {
    val popover = Popover()
    popover.parent = this
    popover.setOffset(offsetX , offsetY)
    popover.hasArrow = false
    onClicked {
        if(active)
            popover.popup()
        else
            popover.popdown()
    }
    popover.popoverHandler()
}

fun title1(text : String) = Label(text).cssClasses("title-1")
fun title2(text : String) = Label(text).cssClasses("title-2")
fun title3(text : String) = Label(text).cssClasses("title-3")
fun title4(text : String) = Label(text).cssClasses("title-4")
fun title5(text : String) = Label(text).cssClasses("title-5")
fun title6(text : String) = Label(text).cssClasses("title-6")

fun vcard(spacing : Int) = vbox(spacing).cssClasses("card")
fun hcard(spacing : Int) = hbox(spacing).cssClasses("card")

fun hseparator() = Separator(Orientation.HORIZONTAL.ordinal)
fun vseparator() = Separator(Orientation.VERTICAL.ordinal)

fun hpane() = Paned(Orientation.HORIZONTAL.ordinal)
fun vpane() = Paned(Orientation.VERTICAL.ordinal)

fun bold(vararg texts : String) = "<b>"+texts.joinToString()+"</b>"
fun italic(vararg texts : String) = "<i>"+texts.joinToString()+"</i>"
fun underline(vararg texts : String) = "<u>"+texts.joinToString()+"</u>"




/*


 */

//
//public class SubList<T>(val list : List<T> , start : Int = 0 , end : Int = -1 , step: Int = 1) : List<T> {
//    val start = start.coerceAtLeast(0)
//    val end = end
//        get() = if(field <= start) {size}
//                else {field}
//    val step = step.coerceAtLeast(0)
//    override val size: Int = (end - start) / step
//    override fun get(index: Int) = list.get(index*step)
//    override fun isEmpty() = list.isEmpty()
//    override fun iterator(): Iterator<T> {
//        TODO("Not yet implemented")
//    }
//
//    override fun listIterator(): ListIterator<T> {
//        TODO("Not yet implemented")
//    }
//
//    override fun listIterator(index: Int): ListIterator<T> {
//        TODO("Not yet implemented")
//    }
//
//    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
//        TODO("Not yet implemented")
//    }
//
//    override fun lastIndexOf(element: T): Int {
//        TODO("Not yet implemented")
//    }
//
//    override fun indexOf(element: T): Int {
//        TODO("Not yet implemented")
//    }
//
//    override fun containsAll(elements: Collection<T>): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun contains(element: T): Boolean {
//        TODO("Not yet implemented")
//    }
//
//}

//public class WidgetProperty<W : Widget, P>(val widgetID : String ,val propertyName : String ,val defaultValue: P) : Property<P>(defaultValue){
//    override fun get() =
//            try {
//                getWidgetByID<W>(widgetID).properties[propertyName]!!.get() as P
//            } catch (_ : Exception) {
//                defaultValue
//            }
//}
//
//fun <T> getPropertyByID(id : String , name : String , defaultValue: T) {
//
//}
//fun <W : Widget> widgetBooleanProperty(widgetID : String , propertyName: String)
//    = WidgetProperty<W , Boolean>(widgetID , propertyName , false)
//
//fun <W : Widget> widgetIntegerProperty(widgetID : String , propertyName: String)
//    = WidgetProperty<W , String>(widgetID , propertyName , false)
//
//fun <W : Widget> widgetBooleanProperty(widgetID : String , propertyName: String)
//    = WidgetProperty<W , Boolean>(widgetID , propertyName , false)
//fun ToggleButton.onToggle(handler : (active : Boolean)->Unit) = this.apply{
//    onToggled {
//        handler(active)
//    }
//}