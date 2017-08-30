package pt.lsts.neptus.plugins.dolphin.dsl
import pt.lsts.nvl.runtime.Position
import pt.lsts.imc.groovy.dsl.Location

/**
 * Position & area operators
 * Conversion between the IMC DSL to generate plans Location class and Dolphin Position class
 * @return Location
 */
Position.metaClass.asType << { 
  Class clazz -> clazz == Location.class ? 
      new Location(delegate.lat * Position.R2D, delegate.lon * Position.R2D) : null; 
}
static main(args) {
     
    x = Position.fromDegrees(35, -8, 0)
    y = x as Location
    
    print y
    NeptusPlatform.INSTANCE.displayMessage 'Neptus language operators extensions loaded!'
  }
