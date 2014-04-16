package edu.vanderbilt.doreguide.model

/**
 * Created by athran on 4/15/14.
 */
sealed trait PlaceCategory {
  def name:     String
  def id:       Int
}

object PlaceCategory {

  def fromName(name: String): PlaceCategory = {
    name match {
      case "Residence Hall" => ResidenceHall
      case "Recreation"     => Recreation
      case "Dining"         => Dining
      case "Academics"      => Academics
      case "Everything"     => Everything
      case "Facility"       => Facility
      case "Medical"        => Medical
      case "Local"          => Local
      case "Athletics"      => Athletics
      case "Greek Life"     => GreekLife
      case "Student Life"   => StudentLife
      case "Library"        => Library
      case _                => Others
    }
  }

  case object ResidenceHall   extends PlaceCategory { def name = "Residence Hall";    def id = 0  }
  case object Recreation      extends PlaceCategory { def name = "Recreation";        def id = 1  }
  case object Dining          extends PlaceCategory { def name = "Dining";            def id = 2  }
  case object Academics       extends PlaceCategory { def name = "Academics";         def id = 3  }
  case object Everything      extends PlaceCategory { def name = "Everything";        def id = 4  }
  case object Facility        extends PlaceCategory { def name = "Facility";          def id = 5  }
  case object Medical         extends PlaceCategory { def name = "Medical";           def id = 6  }
  case object Local           extends PlaceCategory { def name = "Local";             def id = 7  }
  case object Athletics       extends PlaceCategory { def name = "Athletics";         def id = 8  }
  case object GreekLife       extends PlaceCategory { def name = "Greek Life";        def id = 9  }
  case object StudentLife     extends PlaceCategory { def name = "Student Life";      def id = 10 }
  case object Library         extends PlaceCategory { def name = "Library";           def id = 11 }
  case object Others          extends PlaceCategory { def name = "Others";            def id = 12 }
}


