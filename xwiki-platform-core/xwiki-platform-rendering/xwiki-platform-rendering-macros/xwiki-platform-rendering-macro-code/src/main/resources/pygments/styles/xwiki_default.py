from pygments.style import Style
from pygments.styles.default import DefaultStyle
from pygments.token import Keyword, Name, Comment, String, Error, \
     Number, Operator, Generic, Whitespace

__all__ = ['xwiki']

class XWikiStyle(Style):
  """
  Default style from pygments 2.17.2, adapted to fit with a #f5f5f5 background
  We retrieve the default values from
  https://github.com/pygments/pygments/blob/2.4.2/pygments/styles/default.py
  and replace them with those from 
  https://github.com/pygments/pygments/blob/2.18.0/pygments/styles/default.py
  then we adapt a few values to fit the #f5f5f5 background.
  """

  name="xwiki"

  background_color = "#f8f8f8"

  styles = DefaultStyle.styles.copy()
  styles[Comment] = "italic #3C7A7A"
  styles[Comment.Preproc] = "noitalic #9C6500"
  styles[Operator.Word] = "bold #A71FFC"
  styles[Name.Exception] = "bold #CB3F38"
  styles[Name.Label] = "#767600"
  styles[Name.Entity] = "bold #717171"
  styles[Name.Attribute] = "#677721"
  styles[Name.Decorator] = "#A71FFC"
  styles[String.Interpol] = "bold #A45A77"
  styles[String.Escape] = "bold #AA5D1F"
  styles[String.Regex] = "#A45A77"
  styles[Generic.Inserted] = "#008400"
  styles[Generic.Error] = "#E40000"
  styles[Generic.Output] = "#717171"