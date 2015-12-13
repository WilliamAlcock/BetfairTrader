sizeFilter = () -> (x) -> if (x?) && Math.floor(x) != 0 then "£" + Math.floor(x) else ""

filtersModule.filter('sizeFilter', [sizeFilter])