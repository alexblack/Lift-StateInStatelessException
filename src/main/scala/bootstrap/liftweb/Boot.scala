package bootstrap.liftweb

import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.provider._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import _root_.net.liftweb.mapper.{DB, ConnectionManager, Schemifier, DefaultConnectionIdentifier, StandardDBVendor}
import _root_.java.sql.{Connection, DriverManager}
import _root_.com.company.model._

object Session {
  object flag extends SessionVar[Boolean](true)	
}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
	new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
			     Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    // where to search snippet
    LiftRules.addToPackages("com.company")
    Schemifier.schemify(true, Schemifier.infoF _, User)

    // Build SiteMap
    def sitemap() = SiteMap(
      Menu("Home") / "index" >> User.AddUserMenusAfter, // Simple menu form
      // Menu with special Link
      Menu(Loc("Static", Link(List("static"), true, "/static/index"), 
	       "Static Content")))

    LiftRules.setSiteMapFunc(() => User.sitemapMutator(sitemap()))

    setCustomSettings
    
    /*
     * Show the spinny image when an Ajax call starts
     */
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    /*
     * Make the spinny image go away when it ends
     */
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    LiftRules.early.append(makeUtf8)

    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    S.addAround(DB.buildLoanWrapper)
  }
  
  def setCustomSettings {
    LiftRules.dispatch.prepend {
      case _ if Session.flag.is == true => () => Empty
    }    
    
    // Catch 404s
    LiftRules.uriNotFound.prepend { 
      case _ => NotFoundAsTemplate(ParsePath("404" :: Nil, "html", false, false))
    }     
  }

  /**
   * Force the request to be UTF-8
   */
  private def makeUtf8(req: HTTPRequest) {
    req.setCharacterEncoding("UTF-8")
  }
}
