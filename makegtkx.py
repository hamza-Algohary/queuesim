enums = {
    "Align" : {
        "FILL" : 0,
        "START":1,
        "END":2,
        "BASELINE_FILL":3,
        "BASELINE":4,
        "BASELINE_CENTER":5
    } ,

    "Overflow" : {
        "VISIBLE" : 0,
        "HIDDEN"  : 1
    } ,

    "Orientation" : {
        "HORIZONTAL" : 0,
        "VERTICAL"   : 1
    }

}

imports = [
    "import ch.bailu.gtk.gtk.*\n"
    "import ch.bailu.gtk.adw.Avatar\n"
    "import ch.bailu.gtk.adw.ToastOverlay\n"
    "import ch.bailu.gtk.adw.Clamp\n"
    "import ch.bailu.gtk.type.Strs\n"
    "import ch.bailu.gtk.type.Str\n"
]

package_name = "gtkx.gtk"

def enums_as_string():
    gtkx_enums = ""
    for enum in enums:
        gtkx_enums += "enum class "+enum+"{"
        for value in enums[enum]:
            gtkx_enums += value + ","
        gtkx_enums += "}\n\n"
    return gtkx_enums

class Property:
    def __init__(self , full_name:str , default_value:str , converter:str = "" , real_name:str = None , is_real:bool = True , is_enum:bool = False , has_default = True, ) -> None:
        self.full_name = full_name
        self.name , self.type = full_name.split(":")
        self.default_value = default_value
        self.converter = converter if is_enum == False else ".toInt()"
        self.real_name = self.name if real_name is None else real_name
        self.is_real = is_real
        self.is_enum = is_enum
        self.has_default = has_default
        
widget_props = [
    Property("cssClasses:Array<String>" , "arrayOf(\"\")" , converter = ".toStrs()" , is_real=False),
    Property("halign:Align"             , "Align.FILL"    , is_enum=True),
    Property("valign:Align"             , "Align.FILL"    , is_enum=True),
    Property("hexpand:Boolean"          , "true"),
    Property("vexpand:Boolean"          , "true"),
    Property("margins:Int"              , "0" , is_real= False),
    Property("marginTop:Int"            , "margins"),
    Property("marginStart:Int"          , "marginTop"),
    Property("marginBottom:Int"         , "marginTop"),
    Property("marginEnd:Int"            , "marginStart"),
    Property("name:String"              , "uniqueWidgetName()" , converter=".toStr()"),
    Property("opacity:Double"           , "1.0"),
    Property("overflow:Overflow"        , "Overflow.VISIBLE" , is_enum=True),
    Property("width:Int"                , "-1"   , is_real=False),
    Property("height:Int"               , "-1"   , is_real=False),
    Property("style:String"             , "\"\""),
    Property("id:String" , "\"\"" , real_name="ID")
]

class Widget:
    def __init__(self , type_name:str , constructor_params:str = ""  , constructor_tail:str = "", constructor_name:str = None , real_name:str = None , properties:list[Property] = []) -> None:
        self.type_name = type_name
        self.constructor_name = type_name.lower() if constructor_name is None else constructor_name
        self.constructor_params = constructor_params
        self.constructor_tail = constructor_tail + "\t\tthis.setSizeRequest(width , height)\n\t\tthis.func()\n\t\tfor(cssClass in cssClasses){\n\t\t\tthis.addCssClass(cssClass)\n\t\t}\n\t}\n}\n\n"
        self.real_name = type_name if real_name is None else real_name
        self.properties = properties + widget_props 
        self.properties += Property("func:"+type_name+".()->Unit" , "{}" , is_real=False),


    def params_as_string(self):
        params = ""
        for prop in self.properties:
            params += prop.full_name + " = " + prop.default_value + " , "
        return params

    def constructor_head(self):
        return self.type_name+"("+self.constructor_params+")"
    
    def constructor_body(self):
        body = ""
        for prop in self.properties:
            if prop.is_real:
                body += "\t\tthis." + prop.real_name + " = " + prop.name + (".ordinal" if prop.is_enum else prop.converter) + "\n"
        body += self.constructor_tail
        return body

    def __str__(self) -> str:
        return "fun " + self.constructor_name + "(" + self.params_as_string() + ") : " + self.type_name \
                     + "{\n\t return " + self.constructor_head() + ".apply{\n"\
                     + self.constructor_body()
                   
widgets = [
    Widget("Label" , constructor_params="text", properties=[
        Property("text:String" , "\"\"" , real_name="label" , is_real=False),
        Property("useMarkup:Boolean" , "true")
    ]),
    Widget("EditableLabel", constructor_params="text" , properties=[
        Property("text:String" , "\"\"" , real_name="label" , is_real=False)
    ]),
    Widget("TextView"),
    Widget("LinkButton", constructor_params="url" , properties=[
        Property("url:String" , "\"\"" , is_real=False)
    ]),
    Widget("Entry"),
    Widget("PasswordEntry"),
    Widget("SearchEntry"),
    Widget("LevelBar"),
    Widget("ProgressBar"),
    #Widget("Scrollbar" , ),
    #Widget("Scale"),
    #Widget("SpinButton"),
    Widget("ToggleButton"),
    Widget("CheckButton"),
    Widget("Switch"),
    #Widget("DropDown"),
    Widget("Image"),
    Widget("Picture"),
    Widget("Video"),
    Widget("GLArea"),
    Widget("DrawingArea"),
    Widget("Spinner"),
    #Widget("Separator"),
    Widget("Box",constructor_name="hbox",constructor_params="Orientation.HORIZONTAL.ordinal , spacing" , properties= [
        Property("spacing:Int" , "0")
    ]),
    Widget("Box",constructor_name="vbox",constructor_params="Orientation.VERTICAL.ordinal , spacing" , properties= [
        Property("spacing:Int" , "0")
    ]),
    Widget("Button"),
    Widget("HeaderBar"),
    Widget("ScrolledWindow"),
    Widget("Overlay"),
    Widget("Frame", constructor_params="label" , properties=[
        Property("label:String" , "\"\"" , is_real=False)
    ]),
    Widget("Expander", constructor_params="label" , properties=[
        Property("label:String" , "\"\"" , is_real=False)
    ]),
    Widget("WindowHandle"),
    Widget("Clamp"),
    Widget("ToastOverlay"),
    Widget("FlowBox"),
    Widget("Popover")
]

gtkx_header = "package "+package_name+"\n" + "".join(imports)
gtkx_enums = enums_as_string()
gtkx_widgets = "".join(widget.__str__() for widget in widgets)
gtkx_extensions = """

fun String.toStr()         = Str(this)
fun Array<String>.toStrs() = Strs(this)

"""

print(gtkx_header + gtkx_extensions + gtkx_enums + gtkx_widgets)