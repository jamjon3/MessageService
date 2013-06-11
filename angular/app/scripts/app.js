'use strict';

angular.module('angularApp', ['googlechart.directives'])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .when('/dashboard', {
        templateUrl: 'views/dashboard.html',
        controller: 'DashboardCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });

google.load('visualization', '1.0', {packages: ['corechart']});
google.setOnLoadCallback(function () {
    angular.bootstrap(document.body, ['google-chart-sample']);
});