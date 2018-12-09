package com.anylambda.util

import org.slf4j.LoggerFactory

/**
  * Created by leeyh0216 on 17. 4. 23.
  */
trait Logging {
  val logger = LoggerFactory.getLogger(getClass)
}
