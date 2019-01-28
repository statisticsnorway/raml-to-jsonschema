# raml-to-jsonschema

RAML to JSON Schema converts RAML Schema files to JSONSchema.

# Using docker

Usage: 

`docker run -v JSONSCHEMAS_FOLDER:/jsonschemas -v SCHEMAS_FOLDER:/schemas -v EXAMPLES_FOLDER:/examples statisticsnorway/raml-to-jsonschema:latest` 


##Prerequisites

This library uses ```jackson-dataformat-yaml``` which uses SnakeYAML library for low-level YAML parsing.

Yaml has fix set of reserved keywords:
   ```
    y|Y|yes|Yes|YES|n|N|no|No|NO
   |true|True|TRUE|false|False|FALSE
   |on|On|ON|off|Off|OFF
   ```

It has different ways to express a string. F.e
```
 languageCode:
        type: string
        description: The language code. Use only ISO 639-1 codes.
        displayName: Språkkode (ISO 639-1)
        enum:  # TODO: Foreløpig en enum, men bør flyttes til KLASS?
          - nb
          - nn
          - en
          - no
  ```
 
  Above raml will get converted to Json as because of reserved keyword "no"
  ```
  "languageCode" : {
            "type" : "string",
            "description" : "The language code. Use only ISO 639-1 codes.",
            "displayName" : "Språkkode (ISO 639-1)",
            "enum" : [ "nb", "nn", "en", false ]
          }
   ```
  
  ##Solution 
  
  Explicitly provide the type on the elements
  ```
   languageCode:
          type: string
          description: The language code. Use only ISO 639-1 codes.
          displayName: Språkkode (ISO 639-1)
          enum:  # TODO: Foreløpig en enum, men bør flyttes til KLASS?
            - nb
            - nn
            - en
            - !!str no
   ```
 Results
  
 ```
 "languageCode" : {
           "type" : "string",
           "description" : "The language code. Use only ISO 639-1 codes.",
           "displayName" : "Språkkode (ISO 639-1)",
           "enum" : [ "nb", "nn", "en", "no" ]
         }
 ```
 
 For numbers as strings: 
 ```
  valuation?:
         type: string
         enum:  #TODO: Hvilke kategorier har vi her????
           - "1"
           - "2"
           - "3"
           - "4"
           - "5"
         displayName: Verdivurdering
         description: Har noe med sensivitet og bruk å gjøre. kommer fra GDPR-teamet.
 ````
 will result into 
 ```
 "valuation" : {
           "type" : "string",
           "enum" : [ 1, 2, 3, 4, 5 ],
           "displayName" : "Verdivurdering",
           "description" : "Har noe med sensivitet og bruk å gjøre. kommer fra GDPR-teamet."
         }
 ```
 ##Solution
 ```
   valuation?:
          type: string
          enum:  #TODO: Hvilke kategorier har vi her????
            - !!str 1
            - !!str 2
            - !!str 3
            - !!str 4
            - !!str 5
          displayName: Verdivurdering
          description: Har noe med sensivitet og bruk å gjøre. kommer fra GDPR-teamet.
  ```
  will result into 
  ```
  "valuation" : {
            "type" : "string",
            "enum" : [ "1", "2", "3", "4", "5" ],
            "displayName" : "Verdivurdering",
            "description" : "Har noe med sensivitet og bruk å gjøre. kommer fra GDPR-teamet."
          }
  ```
  
  Available types: ``` !!bool, !!str, !!timestamp, !!float, !!int ```
  
  Refer http://yaml.org/type for more details