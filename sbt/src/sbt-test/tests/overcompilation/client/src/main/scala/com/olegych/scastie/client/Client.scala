package com.olegych.scastie.client

import play.api.libs.json._

import org.scalajs.dom.window

object Client {

  def dontSerialize[T](fallback: T): Format[T] = new Format[T] {
    def writes(v: T): JsValue = JsNull
    def reads(json: JsValue): JsResult[T] = JsSuccess(fallback)
  }

  def dontSerializeOption[T]: Format[T] = new Format[T] {
    def writes(v: T): JsValue = JsNull
    def reads(json: JsValue): JsResult[T] = JsSuccess(null.asInstanceOf[T])
  }

  def dontSerializeList[T]: Format[List[T]] =
    dontSerialize(List())

  val isMac: Boolean = window.navigator.userAgent.contains("Mac")
}