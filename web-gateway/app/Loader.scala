import com.example.auction.bidding.api.BiddingService
import com.example.auction.item.api.ItemService
import com.example.auction.user.api.UserService
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.scaladsl.api.{ServiceLocator, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.{ Assets, ItemController, Main, ProfileController }
import play.api.ApplicationLoader.Context
import play.api.http.HttpConfiguration
import play.api.i18n.I18nComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{ ApplicationLoader, BuiltInComponentsFromContext, Mode }
import router.Routes
import com.lightbend.lagom.scaladsl.dns.DnsServiceLocatorComponents


import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class WebGateway(context: Context) extends BuiltInComponentsFromContext(context)
  with I18nComponents
  with AhcWSComponents
  with LagomServiceClientComponents {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "web-gateway",
    Map(
      "web-gateway" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val router = {
    // add the prefix string in local scope for the Routes constructor
    lazy val prefix = "/"
    // ensures the config value play.http.context gets picked up
    wire[Routes] withPrefix HttpConfiguration.fromConfiguration(context.initialConfiguration).context
  }

  lazy val userService = serviceClient.implement[UserService]
  lazy val itemService = serviceClient.implement[ItemService]
  lazy val biddingService = serviceClient.implement[BiddingService]

  lazy val main = wire[Main]
  lazy val itemController = wire[ItemController]
  lazy val profileController = wire[ProfileController]
  lazy val assets = wire[Assets]
}

class WebGatewayLoader extends ApplicationLoader {
  override def load(context: Context) = {
    context.environment.mode match {

      case Mode.Dev =>
        (new WebGateway(context) with LagomDevModeComponents).application

      case _ => new WebGateway(context)  {
        override def serviceLocator = ServiceLocator.NoServiceLocator
      }.application

    }

  }
}
