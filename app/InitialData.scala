import models.{Transformer, User}

/**
 * Author: matthijs 
 * Created on: 24 May 2014.
 *
 * Initial set of data to be loaded
 */
object InitialData {
  def insert() {

    // Create a default user
    val defaultEmail : String = "sendr@localhost"
    User.findByEmail(defaultEmail) match {
      case Some(user) => // Nothing to create
      case None => User.create(defaultEmail,"klJJS13j#k")
    }

    // Create an example transformer
    if (Transformer.all.isEmpty) {
      val soapMessage : String =
        """
          |    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ced="http://www.ced-europe.com/schemas/wsdl/replicatie/CEDWEPF">
          |      <soapenv:Header>
          |        <ced:authentication>
          |          <username>{user}</username>
          |          <password>{password}</password>
          |        </ced:authentication>
          |      </soapenv:Header>
          |      <soapenv:Body>
          |        <ced:CEDWEPF>
          |          <ActionForCEDWEPF>
          |            <Mode>Insert</Mode>
          |            <TimeStampESB>{timestamp}</TimeStampESB>
          |            <TimeStampEIS>{timestamp}</TimeStampEIS>
          |            <SPLCD>{0}</SPLCD>
          |            <WKZOMS>{1}</WKZOMS>
          |          </ActionForCEDWEPF>
          |        </ced:CEDWEPF>
          |      </soapenv:Body>
          |    </soapenv:Envelope>
        """.stripMargin
      Transformer
        .create(new Transformer(1,"CEDWEPPF",null,"/home/sendr/import/cedwepf", ".csv","cp1252","UTF-8",
          "http://localhost:9000/WS/cedwepf","username","secret",10000,soapMessage,"2014-05-24T13:59:00",1))
    }
  }
}

