'use strict';

angular.module('angularApp')
  .controller('NavigationCtrl', function ($scope, $location) {
    $scope.currentPage = $location.path();

    $scope.awesomeThings = [
      'HTML5 Boilerplate',
      'AngularJS',
      'Karma'
    ];
  });