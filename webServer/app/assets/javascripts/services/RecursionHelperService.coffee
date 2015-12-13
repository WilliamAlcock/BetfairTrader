class RecursionHelperService

  constructor: (@$compile) ->

  compile: (element, link) ->
    if angular.isFunction(link)
      link = {post:link}

    # Break the recursion look by removing the contents
    @contents = element.contents().remove()
    @compiledContents = ""
    {
      pre: if (link && link.pre) then link.pre else null

      # Compiles and re-adds the contents
      post: (scope, element) =>
        # Compile the contents
        if !@compiledContents
          @compiledContents = @$compile(@contents)
        # Re-add the compiled contents to the element
        @compiledContents(scope, (clone) -> element.append(clone))
        # Call the post-linking function if any
        if (link && link.post)
          link.post.apply(null, arguments)
    }

servicesModule.service('RecursionHelperService', ['$compile', RecursionHelperService])