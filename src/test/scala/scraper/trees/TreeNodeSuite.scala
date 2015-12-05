package scraper.trees

import scala.collection.JavaConversions._

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.Checkers

import scraper.LoggingFunSuite
import scraper.types.TestUtils

case class Node(value: Int, children: Seq[Node]) extends TreeNode[Node] {
  override def nodeCaption: String = s"Node($value)"
}

class TreeNodeSuite extends LoggingFunSuite with TestUtils with Checkers {
  // A generator that generates trees with a max depth of 10 and a max fan-out of 2.
  private val genNode = {
    def gen(maxDepth: Int): Gen[Node] = for {
      fanOut <- if (maxDepth > 0) Gen.choose(0, 3) else Gen.const(0)
      value <- Gen.posNum[Int]
      children <- Gen.sequence(Seq.fill(fanOut)(gen(maxDepth - 1)))
    } yield Node(value, children)

    gen(maxDepth = 10)
  }

  private implicit val arbNode = Arbitrary(genNode)

  test("transformDown") {
    val tree =
      Node(1, Seq(
        Node(2, Seq(
          Node(4, Nil),
          Node(5, Nil)
        )),
        Node(3, Seq(
          Node(6, Nil),
          Node(7, Nil)
        ))
      ))

    checkTree(
      Node(6, Seq(
        Node(11, Seq(
          Node(4, Nil),
          Node(5, Nil)
        )),
        Node(16, Seq(
          Node(6, Nil),
          Node(7, Nil)
        ))
      )),

      tree.transformDown {
        case child @ Node(i, grandChildren) =>
          child.copy(value = i + grandChildren.map(_.value).sum)
      }
    )
  }

  test("transformUp") {
    val tree =
      Node(1, Seq(
        Node(2, Seq(
          Node(4, Nil),
          Node(5, Nil)
        )),
        Node(3, Seq(
          Node(6, Nil),
          Node(7, Nil)
        ))
      ))

    checkTree(
      Node(28, Seq(
        Node(11, Seq(
          Node(4, Nil),
          Node(5, Nil)
        )),
        Node(16, Seq(
          Node(6, Nil),
          Node(7, Nil)
        ))
      )),

      tree.transformUp {
        case child @ Node(i, grandChildren) =>
          child.copy(value = i + grandChildren.map(_.value).sum)
      }
    )
  }

  test("collect") {
    check { tree: Node =>
      val evenNodes = tree.collect { case node @ Node(i, _) if i % 2 == 0 => node }.toSet
      val oddNodes = tree.collect { case node @ Node(i, _) if i % 2 == 1 => node }.toSet
      val allNodes = tree.collect { case node => node }.toSet

      (evenNodes & oddNodes).isEmpty &&
        (evenNodes ++ oddNodes) == allNodes &&
        evenNodes.forall { _.value % 2 == 0 } &&
        oddNodes.forall { _.value % 2 == 1 }
    }
  }
}
