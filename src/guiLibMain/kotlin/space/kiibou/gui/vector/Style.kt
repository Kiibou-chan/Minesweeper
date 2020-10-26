package space.kiibou.gui.vector

class Style {
    var fill = Color(0, 0, 0, 0)
    var noFill = false
    var stroke = Color(0, 0, 0, 0)
    var noStroke = false
    var strokeWeight = 1
    var strokeJoin = Constants.NONE

    override fun toString(): String {
        return "Style{" +
                "fill=" + fill +
                ", noFill=" + noFill +
                ", stroke=" + stroke +
                ", noStroke=" + noStroke +
                ", strokeWeight=" + strokeWeight +
                '}'
    }
}