
package gtkx

import ch.bailu.gtk.cairo.LineCap
import ch.bailu.gtk.gtk.Box
import ch.bailu.gtk.gtk.Button
import ch.bailu.gtk.gtk.DrawingArea
import ch.bailu.gtk.gtk.HeaderBar
import ch.bailu.gtk.gtk.ToggleButton
import ch.bailu.gtk.gtk.Widget
import gtkx.*

fun main() {
    application("algohary.hamza.QueueSimulator") {
        onActivate {
            window("Queue Simulation").titlebar(HeaderBar().cssClasses("flat")).child(
                    hbox().margins(10).children(
                        hbox().margins(5).cssClasses("linked").children(
                            Button().label("Hello"),
                            ToggleButton().icons("media-pause" , "media-play").hexpand(true).halign(Align.FILL).bind{getWidgetByID<DrawingArea>("drawingArea").animationRunning}
                        ),
                        DrawingArea().id("drawingArea")
                            .margins(5)
                            .overflow(Overflow.HIDDEN)
                            .css("background:blue;border-radius:7px;")
                            .size(100,100)
                            .halign(Align.FILL)
                            .hexpand(true)
                            .onDraw{ width, height ->
                                settings(lineCap = LineCap.ROUND , color = Color(255,0,0) , lineWidth = 10.0)
                                line(0.0,0.0, Math.random()*width , Math.random()*height)
                            }
                    )
                )
            applyStyle()
        }
    }
}
