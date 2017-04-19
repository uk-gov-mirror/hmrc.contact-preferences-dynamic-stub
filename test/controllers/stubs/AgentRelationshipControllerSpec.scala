/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.stubs

import actions.ExceptionTriggersActions
import common.RouteIds
import models.{RelationshipModel, RouteExceptionKeyModel, RouteExceptionModel, SchemaModel}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import repositories._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.{SchemaValidation, TestSchemas}

import scala.concurrent.Future


class AgentRelationshipControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  def setupController(expectedExceptionCode: Option[Int] = None): AgentRelationshipController = {
    val mockCollection = mock[CgtRepository[RelationshipModel, String]]
    val mockRepository = mock[DesAgentClientRelationshipRepository]
    val mockAgentRepo = mock[AgentClientRelationshipRepository]
    val mockExceptionsCollection = mock[CgtRepository[RouteExceptionModel, RouteExceptionKeyModel]]
    val mockExceptionsRepository = mock[RouteExceptionRepository]
    val mockSchemaRepository = mock[SchemaRepository]
    val mockSchemaCollection = mock[CgtRepository[SchemaModel, String]]
    val exceptionTriggersActions = new ExceptionTriggersActions(mockExceptionsRepository)
    val expectedException = expectedExceptionCode.fold(List[RouteExceptionModel]()) {
      code => List(RouteExceptionModel("", "", code))
    }

        when(mockExceptionsRepository.apply())
          .thenReturn(mockExceptionsCollection)

        when(mockExceptionsCollection.findLatestVersionBy(any())(any()))
          .thenReturn(Future.successful(expectedException))

        when(mockRepository.apply())
          .thenReturn(mockCollection)

        when(mockCollection.addEntry(any())(any()))
          .thenReturn(Future.successful({}))

    when(mockSchemaRepository.apply())
      .thenReturn(mockSchemaCollection)

    when(mockSchemaRepository().findLatestVersionBy(any())(any()))
      .thenReturn(Future.successful(List(SchemaModel(RouteIds.createRelationship, TestSchemas.agentRelationshipCreateSchema))))

    val schemaValidation = new SchemaValidation(mockSchemaRepository)

    new AgentRelationshipController(mockAgentRepo, mockRepository, exceptionTriggersActions, schemaValidation)
  }

"Calling createDesAgentClientRelationship" when {
  val validRelationshipModel = RelationshipModel("AARN1274392", "123456789ABCDEF")
  val invalidRelationshipModel = RelationshipModel("2ARN132", "CGT123421")
  val controller = setupController()

  "a valid relationship json payload is sent" should {
    lazy val result = controller.createDesAgentClientRelationship()(FakeRequest("POST", "").withJsonBody(RelationshipModel.asJson(validRelationshipModel)))

    "return a status of NoContent" in {
      status(result) shouldBe 204
    }

    "an invalid relationship json payload is sent" should {
      lazy val result = controller.createDesAgentClientRelationship()(FakeRequest("POST", "").withJsonBody(RelationshipModel.asJson(invalidRelationshipModel)))

      "return a status of 400" in {
        status(result) shouldBe 400
      }
    }
  }
}

}
